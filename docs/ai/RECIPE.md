# AIGC Demo with Agora RTC

## Overview

Run an Agora RTC demo that combines real-time media with AI-generated content workflows.

Repository: [AgoraIO-Community/Agora-AIGCDemo](https://github.com/AgoraIO-Community/Agora-AIGCDemo)

## Prerequisites and Setup

1. Choose a sample before configuring it: cloud Android, cloud Web, or the AI teaching assistant. Each has its own project directory.
2. For cloud Android, open `Agora-AIGCCloud-Demo/Android/Agora-AIGCCloud-Demo` in Android Studio and put `APP_ID` and `APP_CERTIFICATE` in that project's `local.properties`. Sync and run on a device.
3. For cloud Web, enter `Agora-AIGCCloud-Demo/Web/agora-aigccloud-web-example`, copy `.env.example` to `.env`, and fill `VITE_AGORA_APP_ID` and `VITE_AGORA_APP_CERTIFICATE` for isolated testing. Run `npm install` and `npm run dev`; use `npm run build` to check the web bundle.
4. The teaching-assistant Android README additionally requires `KEY`; its iOS README uses `Config.swift`, `pod install`, and `AIGC-Examples.xcworkspace`. Configure the service and prompt resources for the selected sample.

## What To Check

- Verify that the chosen sample starts its AIGC service and produces the expected response to microphone input.
- Stop and restart the session and confirm that capture and service activity end cleanly.

## Integration Notes

These are historical service demos. Service access and compatible SDK artifacts must be available for the chosen sample. The web demo's certificate variable is client-visible; replace that test-only credential flow with a trusted backend before deployment.

Keep App Certificates, customer secrets and privileged token generation on a trusted backend for production. Use only your own test credentials while configuring historical demos; do not commit them. Preserve the sample's existing dependency versions and lifecycle until a separate migration is tested.

## Source References

- [Agora-AIGCCloud-Demo/Android/Agora-AIGCCloud-Demo/README.md](../../Agora-AIGCCloud-Demo/Android/Agora-AIGCCloud-Demo/README.md)
- [Agora-AIGCCloud-Demo/Web/agora-aigccloud-web-example/README.md](../../Agora-AIGCCloud-Demo/Web/agora-aigccloud-web-example/README.md)
- [Agora-AITeachingAssistant/Android/Agora-AITeachingAssistant/README.md](../../Agora-AITeachingAssistant/Android/Agora-AITeachingAssistant/README.md)
- [Agora-AITeachingAssistant/iOS/README.md](../../Agora-AITeachingAssistant/iOS/README.md)

## Validation Status

This recipe was checked against the repository's source and setup documents. The platform build and live service/device flow have not been run as part of this documentation change. Complete the checks above in an environment with the required SDKs, hardware and service access before treating it as a validated deployment.

## Implementation Prompt

```text
You are working in AgoraIO-Community/Agora-AIGCDemo. Use docs/ai/RECIPE.md and its linked source files to adapt the AIGC Demo with Agora RTC reference to this project.

Start by inspecting the actual sample directory and configuration described above. Resolve its listed missing prerequisites before claiming it is runnable. Preserve the existing architecture and compatible dependency versions. Implement the recipe's feature-specific flow, permissions and resource cleanup. Use a trusted backend for privileged credentials and tokens in production.

Run the repository-specific build or launch flow, then perform each What To Check item. Report the commands run, the observed results and anything blocked by service access, hardware or incomplete sample code. Do not claim live validation from a documentation or catalog build alone.
```
