package com.unity3d.player;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// CI-only stub. Mirrors the surface of Unity's real UnityPlayer used by
// SuperwallSDK.androidlib/src/main/kotlin/.../SuperwallUnityBridge.kt so the
// module compiles in isolation. Never shipped — provided as compileOnly only.
public final class UnityPlayer {
    public static Activity currentActivity;

    // Test-only record of every UnitySendMessage call, so the bridge JVM tests can read what C# would receive.
    // Each entry is { gameObject, method, message }.
    public static final List<String[]> sentMessages = Collections.synchronizedList(new ArrayList<String[]>());

    public static void UnitySendMessage(String gameObject, String method, String message) {
        sentMessages.add(new String[] { gameObject, method, message });
    }

    private UnityPlayer() {}
}
