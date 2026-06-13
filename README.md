# NasUX application

[![Build status](https://github.com/nastech-ai/NasTech-Agent/workflows/Build/badge.svg)](https://github.com/nastech-ai/NasTech-Agent/actions)
[![Testing status](https://github.com/nastech-ai/NasTech-Agent/workflows/Unit%20tests/badge.svg)](https://github.com/nastech-ai/NasTech-Agent/actions)
[![Join the chat at https://github.com/nastech-ai/NasTech-Agent](https://img.shields.io/badge/chat-NasTech-blue.svg)](https://github.com/nastech-ai/NasTech-Agent)
[![Join the NasUX discord server](https://img.shields.io/discord/641256914684084234.svg?label=&logo=discord&logoColor=ffffff&color=5865F2)](https://discord.gg/HXpF69X)
[![NasUX library releases at Jitpack](https://jitpack.io/v/nastech-ai/NasTech-Agent.svg)](https://jitpack.io/#nastech-ai/NasTech-Agent)


[NasUX](https://github.com/nastech-ai/NasTech-Agent) is an Android terminal application and Linux environment.

Note that this repository is for the app itself (the user interface and the terminal emulation). For the packages installable inside the app, see [nastech-ai/NasTech-Agent](https://github.com/nastech-ai/NasTech-Agent).

Quick how-to about NasUX package management is available at [Package Management](https://github.com/nastech-ai/NasTech-Agent/wiki/Package-Management). It also has info on how to fix **`repository is under maintenance or down`** errors when running `apt` or `pkg` commands.

**We are looking for NasUX Android application maintainers.**

***

**NOTICE: NasUX may be unstable on Android 12+.** Android OS will kill any (phantom) processes greater than 32 (limit is for all apps combined) and also kill any processes using excessive CPU. You may get `[Process completed (signal 9) - press Enter]` message in the terminal without actually exiting the shell process yourself. Check the related issue [#2366](https://github.com/nastech-ai/NasTech-Agent/issues/2366), [issue tracker](https://issuetracker.google.com/u/1/issues/205156966), [phantom cached and empty processes docs](https://github.com/agnostic-apollo/Android-Docs/blob/master/en/docs/apps/processes/phantom-cached-and-empty-processes.md) and [this TLDR comment](https://github.com/nastech-ai/NasTech-Agent/issues/2366#issuecomment-1237468220) on how to disable trimming of phantom and excessive cpu usage processes. A proper docs page will be added later. An option to disable the killing should be available in Android 12L or 13, so upgrade at your own risk if you are on Android 11, specially if you are not rooted.

***

## Contents
- [NasUX App and Plugins](#nasux-app-and-plugins)
- [Installation](#installation)
- [Uninstallation](#uninstallation)
- [Important Links](#important-links)
- [Debugging](#debugging)
- [For Maintainers and Contributors](#for-maintainers-and-contributors)
- [Forking](#forking)
- [Sponsors and Funders](#sponsors-and-funders)
##



## NasUX App and Plugins

The core [NasUX](https://github.com/nastech-ai/NasTech-Agent) app comes with the following optional plugin apps.

- [NasUX:API](https://github.com/nastech-ai/nasux-api)
- [NasUX:Boot](https://github.com/nastech-ai/nasux-boot)
- [NasUX:Float](https://github.com/nastech-ai/nasux-float)
- [NasUX:Styling](https://github.com/nastech-ai/nasux-styling)
- [NasUX:Tasker](https://github.com/nastech-ai/nasux-tasker)
- [NasUX:Widget](https://github.com/nastech-ai/nasux-widget)
##



## Installation

Latest version is `v0.118.3`.

**NOTICE: It is highly recommended that you update to `v0.118.0` or higher ASAP for various bug fixes, including a critical world-readable vulnerability reported [here](https://nasux.github.io/general/2022/02/15/nasux-apps-vulnerability-disclosures.html). See [below](#google-play-store-experimental-branch) for information regarding NasUX on Google Play.**

NasUX can be obtained through various sources listed below for **only** Android `>= 7` with full support for apps and packages.

Support for both app and packages was dropped for Android `5` and `6` on [2020-01-01](https://www.reddit.com/r/nasux/comments/dnzdbs/end_of_android56_support_on_20200101/) at `v0.83`, however it was re-added just for the app *without any support for package updates* on [2022-05-24](https://github.com/nastech-ai/NasTech-Agent/pull/2740) via the [GitHub](#github) sources. Check [here](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-on-android-5-or-6) for the details.

The APK files of different sources are signed with different signature keys. The `NasUX` app and all its plugins use the same [`sharedUserId`](https://developer.android.com/guide/topics/manifest/manifest-element) `com.nastech.nasux` and so all their APKs installed on a device must have been signed with the same signature key to work together and so they must all be installed from the same source. Do not attempt to mix them together, i.e do not try to install an app or plugin from `F-Droid` and another one from a different source like `GitHub`. Android Package Manager will also normally not allow installation of APKs with different signatures and you will get errors on installation like `App not installed`, `Failed to install due to an unknown error`, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`, `signatures do not match previously installed version`, etc. This restriction can be bypassed with root or with custom roms.

If you wish to install from a different source, then you must **uninstall any and all existing NasUX or its plugin app APKs** from your device first, then install all new APKs from the same new source. Check [Uninstallation](#uninstallation) section for details. You may also want to consider [Backing up NasUX](https://wiki.nasux.dev/wiki/Backing_up_NasUX) before the uninstallation so that you can restore it after re-installing from NasUX different source.

In the following paragraphs, *"bootstrap"* refers to the minimal packages that are shipped with the `nasux-app` itself to start a working shell environment. Its zips are built and released [here](https://github.com/nastech-ai/NasTech-Agent/releases).

### F-Droid

NasUX application can be obtained from `F-Droid` from [here](https://f-droid.org/en/packages/com.nastech.nasux/).

You **do not** need to download the `F-Droid` app (via the `Download F-Droid` link) to install NasUX. You can download the NasUX APK directly from the site by clicking the `Download APK` link at the bottom of each version section.

It usually takes a few days (or even a week or more) for updates to be available on `F-Droid` once an update has been released on `GitHub`. The `F-Droid` releases are built and published by `F-Droid` once they [detect](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.nastech.nasux.yml) a new `GitHub` release. The NasUX maintainers **do not** have any control over the building and publishing of the NasUX apps on `F-Droid`. Moreover, the NasUX maintainers also do not have access to the APK signing keys of `F-Droid` releases, so we cannot release an APK ourselves on `GitHub` that would be compatible with `F-Droid` releases.

The `F-Droid` app often may not notify you of updates and you will manually have to do a pull down swipe action in the `Updates` tab of the app for it to check updates. Make sure battery optimizations are disabled for the app, check https://dontkillmyapp.com/ for details on how to do that.

Only a universal APK is released, which will work on all supported architectures. The APK and bootstrap installation size will be `~180MB`. `F-Droid` does [not support](https://github.com/nastech-ai/NasTech-Agent/pull/1904) architecture specific APKs.

### GitHub

NasUX application can be obtained on `GitHub` either from [`GitHub Releases`](https://github.com/nastech-ai/NasTech-Agent/releases) for version `>= 0.118.0` or from [`GitHub Build Action`](https://github.com/nastech-ai/NasTech-Agent/actions/workflows/debug_build.yml?query=branch%3Amaster+event%3Apush) workflows. **For android `>= 7`, only install `apt-android-7` variants. For android `5` and `6`, only install `apt-android-5` variants.**

The APKs for `GitHub Releases` will be listed under `Assets` drop-down of a release. These are automatically attached when a new version is released.

The APKs for `GitHub Build` action workflows will be listed under `Artifacts` section of a workflow run. These are created for each commit/push done to the repository and can be used by users who don't want to wait for releases and want to try out the latest features immediately or want to test their pull requests. Note that for action workflows, you need to be [**logged into a `GitHub` account**](https://github.com/login) for the `Artifacts` links to be enabled/clickable. If you are using the [`GitHub` app](https://github.com/mobile), then make sure to open workflow link in a browser like Chrome or Firefox that has your GitHub account logged in since the in-app browser may not be logged in.

The APKs for both of these are [`debuggable`](https://developer.android.com/studio/debug) and are compatible with each other but they are not compatible with other sources.

Both universal and architecture specific APKs are released. The APK and bootstrap installation size will be `~180MB` if using universal and `~120MB` if using architecture specific. Check [here](https://github.com/nastech-ai/NasTech-Agent/issues/2153) for details.

**Security warning**: APK files on GitHub are signed with a test key that has been [shared with community](https://github.com/nastech-ai/NasTech-Agent/blob/master/app/testkey_untrusted.jks). This IS NOT an official developer key and everyone can use it to generate releases for own testing. Be very careful when using NasUX GitHub builds obtained elsewhere except https://github.com/nastech-ai/NasTech-Agent. Everyone is able to use it to forge a malicious NasUX update installable over the GitHub build. Think twice about installing NasUX builds distributed via Telegram or other social media. If your device get caught by malware, we will not be able to help you.

The [test key](https://github.com/nastech-ai/NasTech-Agent/blob/master/app/testkey_untrusted.jks) shall not be used to impersonate @nasux and can't be used for this anyway. This key is not trusted by us and it is quite easy to detect its use in user generated content.

<details>
<summary>Keystore information</summary>

```
Alias name: alias
Creation date: Oct 4, 2019
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=APK Signer, OU=Earth, O=Earth
Issuer: CN=APK Signer, OU=Earth, O=Earth
Serial number: 29be297b
Valid from: Wed Sep 04 02:03:24 EEST 2019 until: Tue Oct 26 02:03:24 EEST 2049
Certificate fingerprints:
         SHA1: 51:79:55:EA:BF:69:FC:05:7C:41:C7:D3:79:DB:BC:EF:20:AD:85:F2
         SHA256: B6:DA:01:48:0E:EF:D5:FB:F2:CD:37:71:B8:D1:02:1E:C7:91:30:4B:DD:6C:4B:F4:1D:3F:AA:BA:D4:8E:E5:E1
Signature algorithm name: SHA1withRSA (disabled)
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3
```

</details>

### Google Play Store **(Experimental branch)**

There is currently a build of NasUX available on Google Play for Android 11+ devices, with extensive adjustments in order to pass policy requirements there. This is under development and has missing functionality and bugs (see [here](https://github.com/nasux-play-store/) for status updates) compared to the stable F-Droid build, which is why most users who can should still use F-Droid or GitHub build as mentioned above.

Currently, Google Play will try to update installations away from F-Droid ones. Updating will still fail as [sharedUserId](https://developer.android.com/guide/topics/manifest/manifest-element#uid) has been removed. A planned 0.118.1 F-Droid release will fix this by setting a higher version code than used for the PlayStore app. Meanwhile, to prevent Google Play from attempting to download and then fail to install the Google Play releases over existing installations, you can open the NasUX apps pages on Google Play and then click on the 3 dots options button in the top right and then disable the Enable auto update toggle. However, the NasUX apps updates will still show in the PlayStore app updates list.

If you want to help out with testing the Google Play build (or cannot install NasUX from other sources), be aware that it's built from a separate repository (https://github.com/nasux-play-store/) - be sure to report issues [there](https://github.com/nasux-play-store/nasux-issues/issues/new/choose), as any issues encountered might very well be specific to that repository.

## Uninstallation

Uninstallation may be required if a user doesn't want NasUX installed in their device anymore or is switching to a different [install source](#installation). You may also want to consider [Backing up NasUX](https://wiki.nastech.ai/wiki/Backing_up_NasUX) before the uninstallation.

To uninstall NasUX completely, you must uninstall **any and all existing NasUX or its plugin app APKs** listed in [NasUX App and Plugins](#nasux-app-and-plugins).

Go to `Android Settings` -> `Applications` and then look for those apps. You can also use the search feature if it’s available on your device and search `nasux` in the applications list.

Even if you think you have not installed any of the plugins, it's strongly suggested to go through the application list in Android settings and double-check.
##



## Important Links

### Community
All community links are available [here](https://wiki.nastech.ai/wiki/Community).

The main ones are the following.

- [NasUX Reddit community](https://reddit.com/r/nasux)
- [NasUX User Matrix Channel](https://matrix.to/#/#nasux_nasux:gitter.im) ([Gitter](https://github.com/nastech-ai/NasTech-Agent))
- [NasUX Dev Matrix Channel](https://matrix.to/#/#nasux_dev:gitter.im) ([Gitter](https://gitter.im/nasux/dev))
- [NasUX X (Twitter)](https://twitter.com/nasuxdevs)
- [NasUX Support Email](mailto:support@nasux.dev)

### Wikis

- [NasUX Wiki](https://wiki.nastech.ai/wiki/)
- [NasUX App Wiki](https://github.com/nastech-ai/NasTech-Agent/wiki)
- [NasUX Packages Wiki](https://github.com/nastech-ai/NasTech-Agent/wiki)

### Miscellaneous
- [FAQ](https://wiki.nastech.ai/wiki/FAQ)
- [NasUX File System Layout](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-file-system-layout)
- [Differences From Linux](https://wiki.nastech.ai/wiki/Differences_from_Linux)
- [Package Management](https://wiki.nastech.ai/wiki/Package_Management)
- [Remote Access](https://wiki.nastech.ai/wiki/Remote_Access)
- [Backing up NasUX](https://wiki.nastech.ai/wiki/Backing_up_NasUX)
- [Terminal Settings](https://wiki.nastech.ai/wiki/Terminal_Settings)
- [Touch Keyboard](https://wiki.nastech.ai/wiki/Touch_Keyboard)
- [Android Storage and Sharing Data with Other Apps](https://wiki.nastech.ai/wiki/Internal_and_external_storage)
- [Android APIs](https://wiki.nastech.ai/wiki/NasUX:API)
- [Moved NasUX Packages Hosting From Bintray to IPFS](https://github.com/nastech-ai/NasTech-Agent/issues/6348)
- [Running Commands in NasUX From Other Apps via `RUN_COMMAND` intent](https://github.com/nastech-ai/NasTech-Agent/wiki/RUN_COMMAND-Intent)
- [NasUX and Android 10](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-and-Android-10)


### Terminal

<details>
<summary></summary>

### Terminal resources

- [XTerm control sequences](https://invisible-island.net/xterm/ctlseqs/ctlseqs.html)
- [vt100.net](https://vt100.net/)
- [Terminal codes (ANSI and terminfo equivalents)](https://wiki.bash-hackers.org/scripting/terminalcodes)

### Terminal emulators

- VTE (libvte): Terminal emulator widget for GTK+, mainly used in gnome-terminal. [Source](https://github.com/GNOME/vte), [Open Issues](https://bugzilla.gnome.org/buglist.cgi?quicksearch=product%3A%22vte%22+), and [All (including closed) issues](https://bugzilla.gnome.org/buglist.cgi?bug_status=RESOLVED&bug_status=VERIFIED&chfield=resolution&chfieldfrom=-2000d&chfieldvalue=FIXED&product=vte&resolution=FIXED).

- iTerm 2: OS X terminal application. [Source](https://github.com/gnachman/iTerm2), [Issues](https://gitlab.com/gnachman/iterm2/issues) and [Documentation](https://iterm2.com/documentation.html) (which includes [iTerm2 proprietary escape codes](https://iterm2.com/documentation-escape-codes.html)).

- Konsole: KDE terminal application. [Source](https://projects.kde.org/projects/kde/applications/konsole/repository), in particular [tests](https://projects.kde.org/projects/kde/applications/konsole/repository/revisions/master/show/tests), [Bugs](https://bugs.kde.org/buglist.cgi?bug_severity=critical&bug_severity=grave&bug_severity=major&bug_severity=crash&bug_severity=normal&bug_severity=minor&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&product=konsole) and [Wishes](https://bugs.kde.org/buglist.cgi?bug_severity=wishlist&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&product=konsole).

- hterm: JavaScript terminal implementation from Chromium. [Source](https://github.com/chromium/hterm), including [tests](https://github.com/chromium/hterm/blob/master/js/hterm_vt_tests.js), and [Google group](https://groups.google.com/a/chromium.org/forum/#!forum/chromium-hterm).

- xterm: The grandfather of terminal emulators. [Source](https://invisible-island.net/datafiles/release/xterm.tar.gz).

- Connectbot: Android SSH client. [Source](https://github.com/connectbot/connectbot)

- Android Terminal Emulator: Android terminal app which NasUX terminal handling is based on. Inactive. [Source](https://github.com/jackpal/Android-Terminal-Emulator).
</details>

##



### Debugging

You can help debug problems of the `NasUX` app and its plugins by setting appropriate `logcat` `Log Level` in `NasUX` app settings -> `<APP_NAME>` -> `Debugging` -> `Log Level` (Requires `NasUX` app version `>= 0.118.0`). The `Log Level` defaults to `Normal` and log level `Verbose` currently logs additional information. Its best to revert log level to `Normal` after you have finished debugging since private data may otherwise be passed to `logcat` during normal operation and moreover, additional logging increases execution time.

The plugin apps **do not execute the commands themselves** but send execution intents to `NasUX` app, which has its own log level which can be set in `NasUX` app settings -> `NasUX` -> `Debugging` -> `Log Level`. So you must set log level for both `NasUX` and the respective plugin app settings to get all the info.

Once log levels have been set, you can run the `logcat` command in `NasUX` app terminal to view the logs in realtime (`Ctrl+c` to stop) or use `logcat -d > logcat.txt` to take a dump of the log. You can also view the logs from a PC over `ADB`. For more information, check official android `logcat` guide [here](https://developer.android.com/studio/command-line/logcat).

Moreover, users can generate nasux files `stat` info and `logcat` dump automatically too with terminal's long hold options menu `More` -> `Report Issue` option and selecting `YES` in the prompt shown to add debug info. This can be helpful for reporting and debugging other issues. If the report generated is too large, then `Save To File` option in context menu (3 dots on top right) of `ReportActivity` can be used and the file viewed/shared instead.

Users must post complete report (optionally without sensitive info) when reporting issues. Issues opened with **(partial) screenshots of error reports** instead of text will likely be automatically closed/deleted.

##### Log Levels

- `Off` - Log nothing.
- `Normal` - Start logging error, warn and info messages and stacktraces.
- `Debug` - Start logging debug messages.
- `Verbose` - Start logging verbose messages.
##



## For Maintainers and Contributors

The [nasux-shared](nasux-shared) library was added in [`v0.109`](https://github.com/nastech-ai/NasTech-Agent/releases/tag/v0.109). It defines shared constants and utils of the NasUX app and its plugins. It was created to allow for the removal of all hardcoded paths in the NasUX app. Some of the nasux plugins are using this as well and rest will in future. If you are contributing code that is using a constant or a util that may be shared, then define it in `nasux-shared` library if it currently doesn't exist and reference it from there. Update the relevant changelogs as well. Pull requests using hardcoded values **will/should not** be accepted. NasUX app and plugin specific classes must be added under `com.nastech.nasux.shared.nasux` package and general classes outside it. The [`nasux-shared` `LICENSE`](nasux-shared/LICENSE.md) must also be checked and updated if necessary when contributing code. The licenses of any external library or code must be honoured.

The main NasUX constants are defined by [`NasUXConstants`](https://github.com/nastech-ai/NasTech-Agent/blob/master/nasux-shared/src/main/java/com/nasux/shared/nasux/NasUXConstants.java) class. It also contains information on how to fork NasUX or build it with your own package name. Changing the package name will require building the bootstrap zip packages and other packages with the new `$PREFIX`, check [Building Packages](https://github.com/nastech-ai/NasTech-Agent/wiki/Building-packages) for more info.

Check [NasUX Libraries](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-Libraries) for how to import nasux libraries in plugin apps and [Forking and Local Development](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-Libraries#forking-and-local-development) for how to update nasux libraries for plugins.

The `versionName` in `build.gradle` files of NasUX and its plugin apps must follow the [semantic version `2.0.0` spec](https://semver.org/spec/v2.0.0.html) in the format `major.minor.patch(-prerelease)(+buildmetadata)`. When bumping `versionName` in `build.gradle` files and when creating a tag for new releases on GitHub, make sure to include the patch number as well, like `v0.1.0` instead of just `v0.1`. The `build.gradle` files and `attach_debug_apks_to_release` workflow validates the version as well and the build/attachment will fail if `versionName` does not follow the spec.

### Commit Messages Guidelines

Commit messages **must** use the [Conventional Commits](https://www.conventionalcommits.org) spec so that chagelogs as per the [Keep a Changelog](https://github.com/olivierlacan/keep-a-changelog) spec can automatically be generated by the [`create-conventional-changelog`](https://github.com/nasux/create-conventional-changelog) script, check its repo for further details on the spec. **The first letter for `type` and `description` must be capital and description should be in the present tense.** The space after the colon `:` is necessary. For a breaking change, add an exclamation mark `!` before the colon `:`, so that it is highlighted in the chagelog automatically.

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Only the `types` listed below must be used exactly as they are used in the changelog headings.** For example, `Added: Add foo`, `Added|Fixed: Add foo and fix bar`, `Changed!: Change baz as a breaking change`, etc. You can optionally add a scope as well, like `Fixed(terminal): Fix some bug`. **Do not use anything else as type, like `add` instead of `Added`, etc.**

- **Added** for new features.
- **Changed** for changes in existing functionality.
- **Deprecated** for soon-to-be removed features.
- **Removed** for now removed features.
- **Fixed** for any bug fixes.
- **Security** in case of vulnerabilities.
##



## Forking

- Check [`NasUXConstants`](https://github.com/nastech-ai/NasTech-Agent/blob/master/nasux-shared/src/main/java/com/nasux/shared/nasux/NasUXConstants.java) javadocs for instructions on what changes to make in the app to change package name.
- You also need to recompile bootstrap zip for the new package name. Check [building bootstrap](https://github.com/nastech-ai/NasTech-Agent/wiki/For-maintainers#build-bootstrap-archives), [here](https://github.com/nastech-ai/NasTech-Agent/issues/1983) and [here](https://github.com/nastech-ai/NasTech-Agent/issues/2081#issuecomment-865280111).
- Currently, not all plugins use `NasUXConstants` from `nasux-shared` library and have hardcoded `com.nastech.nasux` values and will need to be manually patched.
- If forking nasux plugins, check [Forking and Local Development](https://github.com/nastech-ai/NasTech-Agent/wiki/NasUX-Libraries#forking-and-local-development) for info on how to use nasux libraries for plugins.
##



## Sponsors and Funders

[<img alt="GitHub Accelerator" width="25%" src="site/assets/sponsors/github.png" />](https://github.com)  
*[GitHub Accelerator](https://github.com/accelerator) ([1](https://github.blog/2023-04-12-github-accelerator-our-first-cohort-and-whats-next))*

&nbsp;

[<img alt="GitHub Secure Open Source Fund" width="25%" src="site/assets/sponsors/github.png" />](https://github.com)  
*[GitHub Secure Open Source Fund](https://resources.github.com/github-secure-open-source-fund) ([1](https://github.blog/open-source/maintainers/securing-the-supply-chain-at-scale-starting-with-71-important-open-source-projects), [2](https://github.com/nastech-ai/NasTech-Agent/en/posts/general/2025/08/11/nasux-selected-for-github-secure-open-source-fund-session-2.html))*

&nbsp;

[<img alt="NLnet NGI Mobifree" width="25%" src="site/assets/sponsors/nlnet-ngi-mobifree.png" />](https://nlnet.nl/mobifree)  
*[NLnet NGI Mobifree](https://nlnet.nl/mobifree) ([1](https://nlnet.nl/news/2024/20241111-NGI-Mobifree-grants.html), [2](https://github.com/nastech-ai/NasTech-Agent/en/posts/general/2024/11/11/nasux-selected-for-nlnet-ngi-mobifree-grant.html))*

&nbsp;

[<img alt="Cloudflare" width="25%" src="site/assets/sponsors/cloudflare.png" />](https://www.cloudflare.com)  
*[Cloudflare](https://www.cloudflare.com) ([1](https://packages-cf.nasux.dev))*

&nbsp;

[<img alt="Warp" width="25%" src="https://github.com/warpdotdev/brand-assets/blob/640dffd347439bbcb535321ab36b7281cf4446c0/Github/Sponsor/Warp-Github-LG-03.png" />](https://www.warp.dev/?utm_source=github&utm_medium=readme&utm_campaign=nasux)  
[*Warp, built for coding with multiple AI agents*](https://www.warp.dev/?utm_source=github&utm_medium=readme&utm_campaign=nasux)
