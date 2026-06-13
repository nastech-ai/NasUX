const { execFileSync } = require('node:child_process');

const variant = process.env.APP_ENV || 'development';
const name = {
    development: "NasTech (dev)",
    preview: "NasTech (preview)",
    production: "NasTech"
}[variant];
const bundleId = {
    development: "ai.nastech.ba.dev",
    preview: "ai.nastech.ba.preview",
    production: "ai.nastech.ba"
}[variant];
const elevenLabsAgentId = process.env.EXPO_PUBLIC_ELEVENLABS_AGENT_ID || null;
const consoleLoggingDefault = {
    development: true,
    preview: true,
    production: false,
}[variant];

function git(args) {
    try {
        return execFileSync('git', args, {
            encoding: 'utf8',
            stdio: ['ignore', 'pipe', 'ignore'],
        }).trim() || undefined;
    } catch {
        return undefined;
    }
}

function loadBuildMetadata() {
    const commitSha =
        process.env.NASTECH_BUILD_COMMIT_SHA ||
        process.env.EAS_BUILD_GIT_COMMIT_HASH ||
        process.env.GITHUB_SHA ||
        git(['rev-parse', 'HEAD']);
    const commitTimestamp =
        process.env.NASTECH_BUILD_COMMIT_TIMESTAMP ||
        (commitSha
            ? git(['show', '-s', '--format=%cI', commitSha])
            : git(['show', '-s', '--format=%cI', 'HEAD']));
    return { commitSha, commitTimestamp };
}

const buildMetadata = loadBuildMetadata();

module.exports = {
    expo: {
        name,
        slug: "nastech-agent",
        version: "1.0.0",
        runtimeVersion: "1",
        orientation: "default",
        icon: './sources/assets/images/icon.png',
        scheme: "nastech",
        userInterfaceStyle: "automatic",
        ios: {
            supportsTablet: true,
            bundleIdentifier: bundleId,
            googleServicesFile: "./GoogleService-Info.plist",
            config: { usesNonExemptEncryption: false },
            infoPlist: {
                NSMicrophoneUsageDescription: "Allow $(PRODUCT_NAME) to access your microphone for voice conversations with AI.",
                NSLocalNetworkUsageDescription: "Allow $(PRODUCT_NAME) to find and connect to local NasTech Agent instances.",
                NSBonjourServices: ["_http._tcp", "_https._tcp"],
                NSAppTransportSecurity: variant === 'production'
                    ? { NSAllowsLocalNetworking: true }
                    : { NSAllowsLocalNetworking: true, NSAllowsArbitraryLoads: true }
            },
            associatedDomains: []
        },
        android: {
            adaptiveIcon: {
                foregroundImage: './sources/assets/images/icon-adaptive.png',
                monochromeImage: './sources/assets/images/icon-monochrome.png',
                backgroundColor: "#18171C"
            },
            permissions: [
                "android.permission.RECORD_AUDIO",
                "android.permission.MODIFY_AUDIO_SETTINGS",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.POST_NOTIFICATIONS",
            ],
            blockedPermissions: [
                "android.permission.ACTIVITY_RECOGNITION",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
            ],
            package: bundleId,
            googleServicesFile: "./google-services.json",
        },
        web: {
            bundler: "metro",
            output: "single",
            favicon: './sources/assets/images/favicon.png'
        },
        plugins: [
            require("./plugins/withEinkCompatibility.js"),
            ["expo-router", { root: "./sources/app" }],
            "expo-asset",
            "expo-localization",
            "expo-mail-composer",
            "expo-secure-store",
            "expo-web-browser",
            "react-native-vision-camera",
            "@more-tech/react-native-libsodium",
            "react-native-audio-api",
            "@livekit/react-native-expo-plugin",
            "@config-plugins/react-native-webrtc",
            ["expo-audio", { microphonePermission: "Allow $(PRODUCT_NAME) to access your microphone for voice conversations." }],
            ["expo-location", {
                locationAlwaysAndWhenInUsePermission: "Allow $(PRODUCT_NAME) to improve AI quality by using your location.",
                locationAlwaysPermission: "Allow $(PRODUCT_NAME) to improve AI quality by using your location.",
                locationWhenInUsePermission: "Allow $(PRODUCT_NAME) to improve AI quality by using your location."
            }],
            ["expo-calendar", { calendarPermission: "Allow $(PRODUCT_NAME) to access your calendar to improve AI quality." }],
            ["expo-camera", {
                cameraPermission: "Allow $(PRODUCT_NAME) to access your camera to scan QR codes and share photos with AI.",
                microphonePermission: "Allow $(PRODUCT_NAME) to access your microphone for voice conversations.",
                recordAudioAndroid: true
            }],
            ["expo-notifications", {
                enableBackgroundRemoteNotifications: true,
                icon: './sources/assets/images/icon-notification.png'
            }],
            ["expo-splash-screen", {
                ios: { backgroundColor: "#F2F2F7", dark: { backgroundColor: "#1C1C1E" } },
                android: {
                    image: './sources/assets/images/splash-android-light.png',
                    backgroundColor: "#F5F5F5",
                    dark: { image: './sources/assets/images/splash-android-dark.png', backgroundColor: "#1e1e1e" }
                }
            }]
        ],
        experiments: { typedRoutes: true },
        extra: {
            router: { root: "./sources/app" },
            eas: {
                projectId: "925ecdd8-07ba-4775-956b-2b334fab8357"
            },
            app: {
                elevenLabsAgentId,
                consoleLoggingDefault,
                buildCommitSha: buildMetadata.commitSha,
                buildCommitTimestamp: buildMetadata.commitTimestamp,
            }
        },
        owner: "naswif-organization"
    }
};
