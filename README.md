README 
======

## License
[GPL v2 or later](https://choosealicense.com/licenses/gpl-2.0/)

## Authors
* Taylor Smock [taylor.smock@kaart.com](mailto:taylor.smock@kaart.com)

## Notes

This plugin is intended to be used as a staging area for new complex JOSM validations.
Each validation MUST have a link to the appropriate JOSM ticket.

As such, _all new code MUST have at least 95% coverage_. Mutation testing is preferred for complex tests (see [PIT](http://pitest.org/)), but these are not checked.

This project also conforms to the JOSM core style guide.


DO NOT reuse functionality from another test. That test may or may not be added to JOSM before your test. It is OK, however, to reuse a parts of a test that is already in JOSM core.
