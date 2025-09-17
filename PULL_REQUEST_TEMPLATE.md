[Link to jira](https://mycore.atlassian.net/browse/MCR-).

# Pull Request Checklist (Author)

Please go through the following checklist before assigning the PR for review:

## Ticket & Documentation
- [ ] The issue in the ticket is clearly described and the solution is documented.
- [ ] Design decisions (if any) are explained.
- [ ] Instructions on how to test or use the feature are included or linked (e.g. to documentation).
- [ ] For UI changes: before & after screenshots are attached.
- [ ] New features or migrations are documented.

## Breaking Changes
- [ ] Does this change affect existing applications, data, or configurations?
  - [ ] Yes: Is a migration required? If yes, describe it.
  - [ ] Breaking change is marked in the **commit message**.

## Versioning
- [ ] The `fixed-version` is correctly set in the ticket and matches the PR's target branch (`main`).

## Bugfix-Specific Checks
- [ ] Affected version is listed in the ticket.
- [ ] Minimal code changes were made (no refactoring).
- [ ] This PR truly fixes **only** the reported bug.
- [ ] A relevant test was added (if feasible).

## Local Testing
- [ ] I have tested the changes locally.
- [ ] The feature behaves as described in the ticket.

## MCR Conventions & Metadata
- [ ] MCR naming conventions are followed.
- [ ] If the **public API** has changed:
  - [ ] Old API is deprecated or a migration is documented.
  - [ ] If not, no action needed.
- [ ] Java license headers are added where necessary.
- [ ] Javadoc is written for non-self-explanatory classes/methods (Clean Code).
- [ ] All configuration options are documented in Javadoc and `mycore.properties`.
- [ ] No default properties are hardcoded — all set via `mycore.properties`.

## Tests
- [ ] Were existing tests modified?
  - [ ] Yes: explain the changes for reviewers.

## Multi-Repo Considerations
- [ ] Is an equivalent PR in `MIR` required?
  - [ ] If yes, is it already created?
