# Contribute to GPSTest

This guide details how to use issues and pull requests (for new code) to improve GPSTest.

## Code Style and Template

We use the [Android Open-Source Project (AOSP) Code Style Guidelines](http://source.android.com/source/code-style.html).

We strongly suggest that you use the `AndroidStyle.xml` template file, included in this repository, in Android Studio to format your code:

1. Place `AndroidStyle.xml` in your Android Studio `/codestyles` directory (e.g., `C:\Users\barbeau\.AndroidStudioPreview\config\codestyles`)
2. Restart Android Studio.
3. Go to "File->Settings->Code Style", and under "Scheme" select "AndroidStyle" and click "Ok".
4. Right-click on the files that contain your contributions and select "Reformat Code", check "Optimize imports", and select "Run".

## Closing policy for issues and pull requests

GPSTest is a popular project and the capacity to deal with issues and pull requests is limited. Out of respect for our volunteers, issues and pull requests not in line with the guidelines listed in this document may be closed without notice.

Please treat our volunteers with courtesy and respect, it will go a long way towards getting your issue resolved.

Issues and pull requests should be in English and contain appropriate language for audiences of all ages.

## Issue tracker

The [issue tracker](https://github.com/barbeau/gpstest/issues) is only for obvious bugs, misbehavior, & feature requests in the latest stable or development release of GPSTest. When submitting an issue please conform to the issue submission guidelines listed below. Not all issues will be addressed and your issue is more likely to be addressed if you submit a pull request which partially or fully addresses the issue.

### Issue tracker guidelines

**[Search](https://github.com/barbeau/gpstest/search?q=&ref=cmdform&type=Issues)** for similar entries before submitting your own, there's a good chance somebody else had the same issue or feature request. Show your support with `:+1:` and/or join the discussion. Please submit issues in the following format (as the first post) and feature requests in a similar format:

1. **Summary:** Summarize your issue in one sentence (what goes wrong, what did you expect to happen)
2. **Steps to reproduce:** How can we reproduce the issue?
3. **Expected behavior:** What did you expect the app to do?
4. **Observed behavior:** What did you see instead?  Describe your issue in detail here.
5. **Device and Android version:** What make and model device (e.g., Samsung Galaxy S3) did you encounter this on?  What Android version (e.g., Android 4.0 Ice Cream Sandwich) are you running?  Is it the stock version from the manufacturer or a custom ROM?
5. **Screenshots:** Can be created by pressing the Volume Down and Power Button at the same time on Android 4.0 and higher.
6. **Possible fixes**: If you can, link to the line of code that might be responsible for the problem.

## Pull requests

We welcome pull requests with fixes and improvements to GPSTest code, tests, and/or documentation. The features we would really like a pull request for are [open issues with the enhancements label](https://github.com/barbeau/gpstest/issues?labels=enhancement&page=1&state=open).

### Pull request guidelines

If you can, please submit a pull request with the fix or improvements including tests. If you don't know how to fix the issue but can write a test that exposes the issue we will accept that as well. In general bug fixes that include a regression test are merged quickly while new features without proper tests are least likely to receive timely feedback. The workflow to make a pull request is as follows:

1. Fork the project on GitHub
2. Create a feature branch
3. Write tests and code
4. Apply the `AndroidStyle.xml` style template to your code in Android Studio.
5. If you have multiple commits please combine them into one commit by [squashing them](http://git-scm.com/book/en/Git-Tools-Rewriting-History#Squashing-Commits)
6. Push the commit to your fork
7. Submit a pull request with a motive for your change and the method you used to achieve it
8. [Search for issues](https://github.com/barbeau/gpstest/search?q=&ref=cmdform&type=Issues) related to your pull request and mention them in the pull request description or comments

We will accept pull requests if:

* The code has proper tests and all tests pass (or it is a test exposing a failure in existing code)
* It can be merged without problems (if not please use: `git rebase master`)
* It doesn't break any existing functionality
* It's quality code that conforms to standard style guides and best practices
* The description includes a motive for your change and the method you used to achieve it
* It is not a catch all pull request but rather fixes a specific issue or implements a specific feature
* It keeps the GPSTest code base clean and well structured
* We think other users will benefit from the same functionality
* If it makes changes to the UI the pull request should include screenshots
* It is a single commit (please use `git rebase -i` to squash commits)

## License

By contributing code to this project via pull requests, patches, or any other process, you are agreeing to license your contributions under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).