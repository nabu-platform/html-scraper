Scraping html is generally tricky because:

- it is usually not valid XML
- it is subject to change on a frequent basis

The problem is that the general approach to html scraping is regex-based. 
However tiny changes in the html layout (which can occur frequently) will break your regexes.

This tool takes a different approach by doing general transformations to the html in order to get a valid xml in the end.
It is less susceptible to changes to the html page.
The scraping process can also be described in an easy to read xml format instead of (sometimes rather complex) regexes.
