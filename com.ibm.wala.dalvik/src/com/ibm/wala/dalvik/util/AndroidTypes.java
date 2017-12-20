/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.util;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 *  Constants for types used by the AndroidModel
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class AndroidTypes {

    public static final TypeName HandlerName = TypeName.string2TypeName("Landroid/os/Handler");
    public static final TypeReference Handler = TypeReference.findOrCreate(ClassLoaderReference.Primordial, HandlerName);

	public static final TypeName IntentName = TypeName.string2TypeName("Landroid/content/Intent");
    public static final TypeReference Intent = TypeReference.findOrCreate(ClassLoaderReference.Primordial, IntentName);
    public static final TypeName ApplicationName = TypeName.string2TypeName("Landroid/app/Application");
    public static final TypeReference Application = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ApplicationName);
    public static final TypeName ActivityName = TypeName.string2TypeName("Landroid/app/Activity");
    public static final TypeReference Activity = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ActivityName);
    public static final TypeName FragmentName = TypeName.string2TypeName("Landroid/app/Fragment");
    public static final TypeReference Fragment = TypeReference.findOrCreate(ClassLoaderReference.Primordial, FragmentName);
    public static final TypeName ServiceName = TypeName.string2TypeName("Landroid/app/Service");
    public static final TypeReference Service = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ServiceName);
    public static final TypeName IntentServiceName = TypeName.string2TypeName("Landroid/app/IntentService");
    public static final TypeReference IntentService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, IntentServiceName);
    public static final TypeName AbstractInputMethodServiceName = TypeName.string2TypeName("Landroid/inputmethodservice/AbstractInputMethodService");
    public static final TypeReference AbstractInputMethodService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, AbstractInputMethodServiceName);
    public static final TypeName AccessibilityServiceName = TypeName.string2TypeName("Landroid/accessibilityservice/AccessibilityService");
    public static final TypeReference AccessibilityService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, AccessibilityServiceName);
    public static final TypeName DreamServiceName = TypeName.string2TypeName("Landroid/service/dreams/DreamService");
    public static final TypeReference DreamService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, DreamServiceName);
    public static final TypeName HostApduServiceName = TypeName.string2TypeName("Landroid/nfc/cardemulation/HostApduService");
    public static final TypeReference HostApduService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, HostApduServiceName);
    public static final TypeName MediaRouteProviderServiceName = TypeName.string2TypeName("Landroid/support/v7/media/MediaRouteProviderService");
    public static final TypeReference MediaRouteProviderService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, MediaRouteProviderServiceName);
    public static final TypeName NotificationListenerServiceName = TypeName.string2TypeName("Landroid/service/notification/NotificationListenerService");
    public static final TypeReference NotificationListenerService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, NotificationListenerServiceName);
    public static final TypeName OffHostApduServiceName = TypeName.string2TypeName("Landroid/nfc/cardemulation/OffHostApduService");
    public static final TypeReference OffHostApduService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, OffHostApduServiceName);
    public static final TypeName PrintServiceName = TypeName.string2TypeName("Landroid/printservice/PrintService");
    public static final TypeReference PrintService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, PrintServiceName);
    public static final TypeName RecognitionServiceName = TypeName.string2TypeName("Landroid/speech/RecognitionService");
    public static final TypeReference RecognitionService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, RecognitionServiceName);
    public static final TypeName RemoteViewsServiceName = TypeName.string2TypeName("Landroid/widget/RemoteViewsService");
    public static final TypeReference RemoteViewsService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, RemoteViewsServiceName);
    public static final TypeName SettingInjectorServiceName = TypeName.string2TypeName("Landroid/location/SettingInjectorService");
    public static final TypeReference SettingInjectorService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, SettingInjectorServiceName);
    public static final TypeName SpellCheckerServiceName = TypeName.string2TypeName("Landroid/service/textservice/SpellCheckerService");
    public static final TypeReference SpellCheckerService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, SpellCheckerServiceName);
    public static final TypeName TextToSpeechServiceName = TypeName.string2TypeName("Landroid/speech/tts/TextToSpeechService");
    public static final TypeReference TextToSpeechService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TextToSpeechServiceName);
    public static final TypeName VpnServiceName = TypeName.string2TypeName("Landroid/net/VpnService");
    public static final TypeReference VpnService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, VpnServiceName);
    public static final TypeName WallpaperServiceName = TypeName.string2TypeName("Landroid/service/wallpaper/WallpaperService");
    public static final TypeReference WallpaperService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, WallpaperServiceName);
    public static final TypeName InputMethodServiceName = TypeName.string2TypeName("Landroid/inputmethodservice/InputMethodService");
    public static final TypeReference InputMethodService = TypeReference.findOrCreate(ClassLoaderReference.Primordial, InputMethodServiceName);
    public static final TypeName ContentProviderName = TypeName.string2TypeName("Landroid/content/ContentProvider");
    public static final TypeReference ContentProvider = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContentProviderName);
    public static final TypeName BroadcastReceiverName = TypeName.string2TypeName("Landroid/content/BroadcastReceiver");
    public static final TypeReference BroadcastReceiver = TypeReference.findOrCreate(ClassLoaderReference.Primordial, BroadcastReceiverName);
    public static final TypeName ContentResolverName = TypeName.string2TypeName("Landroid/content/ContentResolver");
    public static final TypeReference ContentResolver = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContentResolverName);

    public static final TypeName MenuName = TypeName.findOrCreate("Landroid/view/Menu");
    public static final TypeReference Menu = TypeReference.findOrCreate(ClassLoaderReference.Primordial, MenuName);
    public static final TypeName ContextMenuName = TypeName.findOrCreate("Landroid/view/ContextMenu");
    public static final TypeReference ContextMenu = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContextMenuName);
    public static final TypeName MenuItemName = TypeName.findOrCreate("Landroid/view/MenuItem");
    public static final TypeReference MenuItem = TypeReference.findOrCreate(ClassLoaderReference.Primordial, MenuItemName);

    public static final TypeName TelephonyManagerName = TypeName.findOrCreate("Landroid/telephony/TelephonyManager");
    public static final TypeReference TelephonyManager = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TelephonyManagerName);

    public static final TypeName ActionModeName = TypeName.findOrCreate("Landroid/view/ActionMode");
    public static final TypeReference ActionMode = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ActionModeName);
    public static final TypeName AttributeSetName = TypeName.findOrCreate("Landroid/util/AttributeSet");
    public static final TypeReference AttributeSet = TypeReference.findOrCreate(ClassLoaderReference.Primordial, AttributeSetName);
    public static final TypeName ActionModeCallbackName = TypeName.findOrCreate("Landroid/view/ActionMode$Callback");
    public static final TypeReference ActionModeCallback = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ActionModeCallbackName);

    public static final TypeName BundleName = TypeName.findOrCreate("Landroid/os/Bundle");
    public static final TypeReference Bundle = TypeReference.findOrCreate(ClassLoaderReference.Primordial, BundleName);
    public static final TypeName IntentSenderName = TypeName.findOrCreate("Landroid/content/IntentSender");
    public static final TypeReference IntentSender = TypeReference.findOrCreate(ClassLoaderReference.Primordial, IntentSenderName);
    public static final TypeName IIntentSenderName = TypeName.findOrCreate("Landroid/content/IIntentSender");
    public static final TypeReference IIntentSender = TypeReference.findOrCreate(ClassLoaderReference.Primordial, IntentSenderName);
    public static final TypeName IBinderName = TypeName.findOrCreate("Landroid/os/IBinder");
    public static final TypeReference IBinder = TypeReference.findOrCreate(ClassLoaderReference.Primordial, IBinderName);
    public static final TypeName ActivityThreadName = TypeName.findOrCreate("Landroid/app/ActivityThread");
    public static final TypeReference ActivityThread = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ActivityThreadName);

    public static final TypeName ContextName = TypeName.findOrCreate("Landroid/content/Context");
    public static final TypeReference Context = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContextName);
    public static final TypeName ContextWrapperName = TypeName.findOrCreate("Landroid/content/ContextWrapper");
    public static final TypeReference ContextWrapper = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContextWrapperName);
    public static final TypeName ContextImplName = TypeName.findOrCreate("Landroid/app/ContextImpl");
    public static final TypeReference ContextImpl = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContextImplName);
    public static final TypeName BridgeContextName = TypeName.findOrCreate("Lcom/android/layoutlib/bridge/android/BridgeContext");
    public static final TypeReference BridgeContext = TypeReference.findOrCreate(ClassLoaderReference.Primordial, BridgeContextName);
    public static final TypeName ContextThemeWrapperName = TypeName.findOrCreate("Landroid/view/ContextThemeWrapper");
    public static final TypeReference ContextThemeWrapper = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ContextThemeWrapperName);

    public static final TypeName PolicyManagerName = TypeName.findOrCreate("Lcom/android/internal/policy/PolicyManager");
    public static final TypeReference PolicyManager = TypeReference.findOrCreate(ClassLoaderReference.Primordial, PolicyManagerName);
    public static final TypeName WindowName = TypeName.findOrCreate("Landroid/view/Window");
    public static final TypeReference Window = TypeReference.findOrCreate(ClassLoaderReference.Primordial, WindowName);

    public static final TypeName UserHandleName = TypeName.findOrCreate("Landroid/os/UserHandle");
    public static final TypeReference UserHandle = TypeReference.findOrCreate(ClassLoaderReference.Primordial, UserHandleName);

    public static final TypeName LoadedApkName = TypeName.findOrCreate("Landroid/app/LoadedApk");
    public static final TypeReference LoadedApk = TypeReference.findOrCreate(ClassLoaderReference.Primordial, LoadedApkName);

    public static final TypeName ResourcesName = TypeName.findOrCreate("Landroid/content/res/Resources");
    public static final TypeReference Resources = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ResourcesName);

    public static final TypeName InstrumentationName = TypeName.findOrCreate("Landroid/app/Instrumentation");
    public static final TypeReference Instrumentation = TypeReference.findOrCreate(ClassLoaderReference.Primordial, InstrumentationName);

    public static final TypeName ActivityInfoName = TypeName.findOrCreate("Landroid/content/pm/ActivityInfo");
    public static final TypeReference ActivityInfo = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ActivityInfoName);

    public static final TypeName ConfigurationName = TypeName.findOrCreate("Landroid/content/res/Configuration");
    public static final TypeReference Configuration = TypeReference.findOrCreate(ClassLoaderReference.Primordial, ConfigurationName);

    public static final TypeName KeyEventName = TypeName.findOrCreate("Landroid/view/KeyEvent");

    public enum AndroidContextType {
        CONTEXT_IMPL,
        CONTEXT_BRIDGE,
        ACTIVITY,
        APPLICATION,
        SERVICE,
        /**
         *  For internal use during development
         */
        USELESS
    }
}
