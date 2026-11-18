# UI requests to the service layer

Written by UI-ENG. The UI works without these. Nothing here changes the services.

1. **Time to acknowledge (shift report KPI).** The shift report needs the time from OPENED to ACKNOWLEDGED per incident. `IncidentService.list` returns no action times. Today the UI can only get them one incident at a time through `detail`. Request: an `IncidentRow` field `acknowledgedAt` or a bulk action query for a filter.
2. **Role check on assignments.** `AssignmentService.assign`, `endAssignment` and `setZone` check site access only, not role. The UI offers these to SITE_MANAGER and SUPERVISOR only. Request: the service should enforce the same rule, or the TL should confirm the rule is UI-only.
3. **Worker list without paging on People and helmets.** The screen loads up to 500 workers in one query. A count or a stable total would help for larger sites.
4. **Helmet firmware and last boot.** People and helmets shows neither. Not in `HelmetRow`. Add if the TL wants the column.
5. **Shift start time.** Worker detail cannot warn when a zone was set before the shift started. There is no shift start in the data model.
