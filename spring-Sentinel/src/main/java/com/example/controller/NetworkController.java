/**
 * NetworkController
 *
 * PURPOSE: Read-side REST API for the network-analysis feature (see
 *          notes/RiskLogic.md "Graph Network Risk Logic"). This controller
 *          never computes graph metrics itself - all scoring/community
 *          detection/PageRank happens in the separate, periodically-run
 *          Python/NetworkX job (see network-analysis/ at the repo root),
 *          which writes its results into network_runs/account_network_scores.
 *          Spring's job here is purely to serve that data plus the small
 *          on-demand "shared payee neighborhood" subgraph, which IS cheap
 *          enough to compute live from the transactions table.
 *
 * ENDPOINTS:
 *   - GET  /api/network/scores                      Ranked accounts from the latest completed run
 *   - GET  /api/network/accounts/{id}                Latest score + evidence + score-over-time timeline
 *   - GET  /api/network/accounts/{id}/graph          Small (LIMIT-bounded) shared-payee neighborhood subgraph
 *   - GET  /api/network/runs                         Run history (freshness/staleness for the operator)
 *   - POST /api/network/analysis/run                 Operator-triggered "Run Analysis Now" (DB-mediated, see NetworkRunRequest)
 */
package com.example.controller;

import com.example.dto.NetworkAccountDetailResponse;
import com.example.dto.NetworkGraphResponse;
import com.example.dto.NetworkRunResponse;
import com.example.dto.NetworkScoreResponse;
import com.example.dto.NetworkTimelinePoint;
import com.example.entity.Account;
import com.example.entity.AccountNetworkScore;
import com.example.entity.NetworkRun;
import com.example.enums.NetworkRunStatus;
import com.example.repository.AccountNetworkScoreRepository;
import com.example.repository.AccountRepository;
import com.example.repository.NetworkRunRepository;
import com.example.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/network")
public class NetworkController {

    private final NetworkRunRepository networkRunRepository;
    private final AccountNetworkScoreRepository scoreRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // How far back the on-demand /graph neighborhood query looks - separate from
    // (and typically shorter than) the batch job's own lookback-days, since this
    // is a live query hitting the transactions table directly.
    @Value("${network.graph.lookback-days:90}")
    private int graphLookbackDays;

    @Value("${network.graph.max-neighbors:30}")
    private int graphMaxNeighbors;

    // Direct-execution config for "Run Analysis Now" - see requestRun() javadoc below
    // for why this deliberately runs the Python job synchronously rather than through
    // the network_run_requests polling queue.
    @Value("${network.python.executable}")
    private String pythonExecutable;

    @Value("${network.python.script-dir}")
    private String pythonScriptDir;

    @Value("${network.python.timeout-seconds:120}")
    private int pythonTimeoutSeconds;

    // Reuse Spring's active datasource credentials for the Python job so
    // local .env setup is optional and both runtimes stay in sync.
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    public NetworkController(NetworkRunRepository networkRunRepository,
                              AccountNetworkScoreRepository scoreRepository,
                              AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.networkRunRepository = networkRunRepository;
        this.scoreRepository = scoreRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/scores")
    public ResponseEntity<Page<NetworkScoreResponse>> getScores(
            @RequestParam(required = false) BigDecimal minScore,
            @PageableDefault(size = 50) Pageable pageable) {
        Optional<NetworkRun> latestRun = networkRunRepository.findFirstByStatusOrderByCompletedAtDesc(NetworkRunStatus.COMPLETED);
        if (latestRun.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }
        Integer runId = latestRun.get().getRunId();
        Page<AccountNetworkScore> page = (minScore != null)
                ? scoreRepository.findByRunIdAndNetworkRiskScoreGreaterThanEqualOrderByNetworkRiskScoreDesc(runId, minScore, pageable)
                : scoreRepository.findByRunIdOrderByNetworkRiskScoreDesc(runId, pageable);
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<NetworkAccountDetailResponse> getAccountDetail(@PathVariable("id") Integer accountId) {
        List<AccountNetworkScore> history = scoreRepository.findByAccountIdOrderByComputedAtAsc(accountId);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        NetworkScoreResponse latest = toResponse(history.get(history.size() - 1));
        List<NetworkTimelinePoint> timeline = history.stream()
                .map(s -> new NetworkTimelinePoint(s.getRunId(), s.getComputedAt(), s.getNetworkRiskScore()))
                .toList();
        return ResponseEntity.ok(new NetworkAccountDetailResponse(latest, timeline));
    }

    /**
     * Small local neighborhood subgraph, not the whole network - see
     * TransactionRepository.findSharedPayeeNeighbors. Edges represent
     * accounts sharing at least one payee within graphLookbackDays, weighted
     * by how many payees they share (a bipartite PROJECTION, not literal
     * account-to-account transfers).
     */
    @GetMapping("/accounts/{id}/graph")
    public ResponseEntity<NetworkGraphResponse> getAccountGraph(@PathVariable("id") Integer accountId) {
        Optional<Account> center = accountRepository.findById(accountId);
        if (center.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(graphLookbackDays);
        List<TransactionRepository.SharedPayeeNeighbor> neighbors =
                transactionRepository.findSharedPayeeNeighbors(accountId, since, PageRequest.of(0, graphMaxNeighbors));

        List<Integer> neighborIds = neighbors.stream().map(n -> n.getNeighborId()).toList();
        Map<Integer, Account> neighborAccounts = new HashMap<>();
        accountRepository.findAllById(neighborIds).forEach(a -> neighborAccounts.put(a.getAccountId(), a));

        // Latest network risk score (if any) for every node shown, so the frontend can color nodes by risk.
        Optional<NetworkRun> latestRun = networkRunRepository.findFirstByStatusOrderByCompletedAtDesc(NetworkRunStatus.COMPLETED);
        Map<Integer, BigDecimal> scoreByAccount = new HashMap<>();
        if (latestRun.isPresent()) {
            scoreRepository.findByRunIdAndAccountId(latestRun.get().getRunId(), accountId)
                    .ifPresent(s -> scoreByAccount.put(accountId, s.getNetworkRiskScore()));
            for (Integer neighborId : neighborIds) {
                scoreRepository.findByRunIdAndAccountId(latestRun.get().getRunId(), neighborId)
                        .ifPresent(s -> scoreByAccount.put(neighborId, s.getNetworkRiskScore()));
            }
        }

        List<NetworkGraphResponse.NetworkGraphNode> nodes = new java.util.ArrayList<>();
        nodes.add(new NetworkGraphResponse.NetworkGraphNode(
                accountId, center.get().getAccountNumber(), true, scoreByAccount.get(accountId)));
        for (Integer neighborId : neighborIds) {
            Account a = neighborAccounts.get(neighborId);
            nodes.add(new NetworkGraphResponse.NetworkGraphNode(
                    neighborId, a != null ? a.getAccountNumber() : null, false, scoreByAccount.get(neighborId)));
        }

        List<NetworkGraphResponse.NetworkGraphEdge> edges = neighbors.stream()
                .map(n -> new NetworkGraphResponse.NetworkGraphEdge(accountId, n.getNeighborId(), n.getSharedPayees()))
                .toList();

        return ResponseEntity.ok(new NetworkGraphResponse(accountId, nodes, edges));
    }

    @GetMapping("/runs")
    public ResponseEntity<Page<NetworkRunResponse>> getRuns(@PageableDefault(size = 20) Pageable pageable) {
        Page<NetworkRun> runs = networkRunRepository.findAllByOrderByStartedAtDesc(pageable);
        return ResponseEntity.ok(runs.map(r -> new NetworkRunResponse(
                r.getRunId(), r.getStartedAt(), r.getCompletedAt(), r.getStatus(), r.getTriggerType(),
                r.getLookbackDays(), r.getAlgorithmVersion(), r.getAccountsAnalyzed(), r.getAccountsFlagged(), r.getErrorMessage())));
    }

    /**
     * Operator-triggered "Run Analysis Now". Directly launches the Python
     * batch job (run_analysis.py) as a subprocess and BLOCKS until it
     * finishes, then returns the actual run result (accountsAnalyzed/
     * accountsFlagged/status/errorMessage) - not just an "accepted" message.
     *
     * This intentionally trades strict Java/Python decoupling for a much
     * simpler, more honest UX: for this project's data volume the whole
     * analysis finishes in a couple of seconds, so blocking one HTTP
     * request thread for that long is a reasonable simplification rather
     * than requiring a separately-run scheduler.py polling loop just to
     * react to a manual click. The network_run_requests table/queue is
     * still used for scheduler.py's own periodic (SCHEDULED) runs if you
     * choose to run it, but the manual button no longer depends on it.
     */
    @PostMapping("/analysis/run")
    public ResponseEntity<Map<String, Object>> requestRun(@RequestBody(required = false) Map<String, Integer> body) {
        Integer lookbackDays = (body != null && body.get("lookbackDays") != null) ? body.get("lookbackDays") : 30;

        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable, "run_analysis.py",
                "--lookback-days", String.valueOf(lookbackDays),
                "--trigger", "MANUAL");
        pb.directory(new File(pythonScriptDir));
        pb.redirectErrorStream(true);

        applyDatabaseEnvironment(pb);

        String output;
        int exitCode;
        try {
            Process process = pb.start();
            // Must drain stdout while waiting, or the child process can block forever
            // once its output buffer fills up.
            output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            boolean finished = process.waitFor(pythonTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of(
                        "status", "TIMED_OUT",
                        "message", "network-analysis job did not finish within " + pythonTimeoutSeconds + "s.",
                        "output", output
                ));
            }
            exitCode = process.exitValue();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "message", "Could not launch network-analysis job: " + e.getMessage()
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "FAILED",
                    "message", "Interrupted while waiting for network-analysis job."
            ));
        }

        // run_analysis.py always writes its own network_runs row (COMPLETED or FAILED)
        // regardless of exit code, so the most recently started run is the one we just ran.
        Optional<NetworkRun> justRan = networkRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 1))
                .stream().findFirst();

        Map<String, Object> response = new HashMap<>();
        response.put("exitCode", exitCode);
        response.put("output", output);
        if (justRan.isPresent()) {
            NetworkRun run = justRan.get();
            response.put("runId", run.getRunId());
            response.put("status", run.getStatus());
            response.put("accountsAnalyzed", run.getAccountsAnalyzed());
            response.put("accountsFlagged", run.getAccountsFlagged());
            response.put("completedAt", run.getCompletedAt());
            response.put("errorMessage", run.getErrorMessage());
            response.put("message", run.getStatus() == NetworkRunStatus.COMPLETED
                    ? "Analysis complete - " + run.getAccountsAnalyzed() + " accounts analyzed, "
                            + run.getAccountsFlagged() + " flagged."
                    : "Analysis failed - see errorMessage.");
            boolean ok = exitCode == 0 && run.getStatus() == NetworkRunStatus.COMPLETED;
            return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        response.put("status", "UNKNOWN");
        response.put("message", "Job process finished (exit code " + exitCode + ") but no network_runs row was found.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private NetworkScoreResponse toResponse(AccountNetworkScore s) {
        String accountNumber = accountRepository.findById(s.getAccountId()).map(a -> a.getAccountNumber()).orElse(null);
        return new NetworkScoreResponse(
                s.getRunId(), s.getAccountId(), accountNumber, s.getNetworkRiskScore(),
                s.getPageRankPercentile(), s.getSharedPayeeCount(), s.getCommunityId(), s.getCommunitySize(),
                s.getGrowthScore(), s.getFraudExposureScore(), s.getEvidenceJson(), s.getNetworkReason(), s.getComputedAt());
    }

    private void applyDatabaseEnvironment(ProcessBuilder pb) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            return;
        }

        Matcher matcher = Pattern.compile("^jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+).*$").matcher(datasourceUrl.trim());
        if (!matcher.matches()) {
            return;
        }

        String dbHost = matcher.group(1);
        String dbPort = matcher.group(2) != null ? matcher.group(2) : "3306";
        String dbName = matcher.group(3);

        Map<String, String> env = pb.environment();
        env.put("NETWORK_DB_HOST", dbHost);
        env.put("NETWORK_DB_PORT", dbPort);
        env.put("NETWORK_DB_NAME", dbName);
        env.put("NETWORK_DB_USER", datasourceUsername);
        env.put("NETWORK_DB_PASSWORD", datasourcePassword != null ? datasourcePassword : "");
    }
}
