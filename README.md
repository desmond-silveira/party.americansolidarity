# Multi-winner Approval Voting Ballot Counter

This program reads either approval-style ballots from a CSV file or
ranked-style ballots from a BLT file.  It returns the tallied results to the
console using
* [Approval Voting](https://en.wikipedia.org/wiki/Approval_voting)
* Net Approval Voting
* [Satisfaction Approval Voting](https://en.wikipedia.org/wiki/Satisfaction_approval_voting)
* [Proportional Approval Voting](https://en.wikipedia.org/wiki/Proportional_approval_voting)
* [Sequential Proportional Approval Voting](https://en.wikipedia.org/wiki/Sequential_proportional_approval_voting)
* [Single Transferable Vote](https://en.wikipedia.org/wiki/Single_transferable_vote)
using [Droop quota](https://en.wikipedia.org/wiki/Droop_quota) and
[Wright](https://en.wikipedia.org/wiki/Wright_system) redistribution

## Build

To build the software from the source code, make sure that you have at least
Java 9, though I recommend installing the latest version of Java.  To check
which Java have installed, if any, run the following command from the command
line:

`java --version`

In addition to Java, building the software from scratch requires Maven, which
can be downloaded from:

https://maven.apache.org/

Once all the project has been downloaded into a directory, and both Java and
Maven have been installed, build using the following command from within the
directory with the project:

`mvn package`

The result of the build will be placed in the `target` subdirectory.

## Running the Project

Once the software has been built or downloaded in JAR form, run the project
using the following command:

`java -jar ./target/ballot-counter-0.0.3.jar <filepath> [seat_count] [max_proportional_slates]`

where <filepath> is the path to the ballot data file.

Optionally, include the number of seats to be filled and the max number of
proportional slates to be returned in the results.

## Data Files

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

The BLT file format is that first described by Hill, Wichmann & Woodall in
_Algorithm 123 - Single Transferable Vote by Meek's method_ (1987) and
popularized by OpenSTV.

Copyright © 2018 Desmond Silveira.  All rights reserved.

This software is free to use and extend to all registered members of the
American Solidarity Party.  Any extensions must give credit to the author
and preserve this notice.
