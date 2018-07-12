# Multi-winner Approval Voting Ballot Counter

This program reads a CSV file of ballots and returns the tallied results using
* [Approval Voting](https://en.wikipedia.org/wiki/Approval_voting)
* [Proportional Approval Voting](https://en.wikipedia.org/wiki/Proportional_approval_voting)
* [Satisfaction Approval Voting](https://en.wikipedia.org/wiki/Satisfaction_approval_voting)

The format of the CSV file consists of a header line with all of the
candidates followed by lines representing each ballot. A non-empty entry in
the same index location (column) as the candidate in the header line
indicates a vote for that candidate. Example:

```
Hillary Clinton,Donald Trump,Gary Johnson,Jill Stein,Mike Maturen
,2,,,5
1,,,,
,,3,4,5
```

indicates that there are five candidates and three ballots with the first
ballot containing votes for Trump and Maturen, the second ballot containing a
vote for Clinton, and the third ballot containing votes for Johnson, Stein,
and Maturen.

Note that this format is easily exportable from [SurveyMonkey](http://www.surveymonkey.com).