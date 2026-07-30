When an alert is created, save the risk score it had at that exact moment, plus which rule caused it and what data triggered it (transaction ids). 
This score should never change later, so anyone looking back knows exactly how serious it looked at the start.



Then, every time an operator does something new with the alert (acknowledge, investigate, close, dismiss)

log:
what the status changed from and to
the risk score at that specific moment (it can rise over time if the alert sits unresolved)
how long it sat in the previous status before this action
a reason code chosen from a fixed list, like confirmed fraud, false positive known customer, false positive rule too sensitive, legitimate large purchase, duplicate alert, insufficient evidence
a free text note explaining the decision in the operator's own words

something like:

opened at risk 78
acknowledged 3 minutes later, same risk
investigating 2 minutes after that
closed 34 minutes later at risk 82, reason confirmed fraud, note explaining what was found

That way, anyone reviewing the alert afterward sees not just what happened, but why, and how urgent it looked at each step.

Allow reopening a closed alert if new information comes in, instead of only allowing forward moves.