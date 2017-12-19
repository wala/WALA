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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;
/**
 *  Android Components like Activity, Service, ...
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-16
 */
public enum AndroidComponent {
    APPLICATION(AndroidTypes.Application, "Application"),
    ACTIVITY(AndroidTypes.Activity, "Activity"),
    FRAGMENT(AndroidTypes.Fragment, "Fragment"),
    SERVICE(AndroidTypes.Service, "Service"),
    INTENT_SERVICE(AndroidTypes.IntentService, "IntentService"),
    ABSTRACT_INPUT_METHOD_SERVICE(AndroidTypes.AbstractInputMethodService, "AbstractInputMethodService"),
    ACCESSIBILITY_SERVICE(AndroidTypes.AccessibilityService, "AccessibilityService"),
    DREAM_SERVICE(AndroidTypes.DreamService, "DreamService"),
    HOST_APDU_SERVICE(AndroidTypes.HostApduService, "HostApduService"),
    MEDIA_ROUTE_PROVIDER_SERVICE(AndroidTypes.MediaRouteProviderService, "MediaRouteProviderService"),
    NOTIFICATION_LISTENER_SERVICE(AndroidTypes.NotificationListenerService, "NotificationListenerService"),
    OFF_HOST_APDU_SERVICE(AndroidTypes.OffHostApduService, "OffHostApduService"),
    PRINT_SERVICE(AndroidTypes.PrintService, "PrintService"),
    RECOGNITION_SERVICE(AndroidTypes.RecognitionService, "RecognitionService"),
    REMOTE_VIEWS_SERVICE(AndroidTypes.RemoteViewsService, "RemoteViewsService"),
    SETTING_INJECTOR_SERVICE(AndroidTypes.SettingInjectorService, "SettingInjectorService"),
    SPELL_CHECKER_SERVICE(AndroidTypes.SpellCheckerService, "SpellCheckerService"),
    TEXT_TO_SPEECH_SERVICE(AndroidTypes.TextToSpeechService, "TextToSpeechService"),
    VPN_SERVICE(AndroidTypes.VpnService, "VpnService"),
    WALLPAPER_SERVICE(AndroidTypes.WallpaperService, "WallpaperService"),
    INPUT_METHOD_SERVICE(AndroidTypes.InputMethodService, "InputMethodService"),
    PROVIDER(AndroidTypes.ContentProvider, "ContentProvider"),
    BROADCAST_RECEIVER(AndroidTypes.BroadcastReceiver, "BroadcastReceiver"),
    // Additional CallBacks:
    LOADER_CB("Landroid/app/LoaderManager/LoaderCallbacks", "CallBackFromLoader"),
    RESOLVER (AndroidTypes.ContentResolver, "ContentResolver"),
    CONTEXT (AndroidTypes.Context, "Context"),
    HTTP ("Landroid/net/AndroidHttpClient", "AndroidHttpClient"),
    BINDER (AndroidTypes.IBinder, "IBinder"),
    LOCATION_MGR ("Landroid/location/LocationManager", "LocationManager"),
    TELEPHONY ("Landroid/telephony/TelephonyManager", "TelephonyManager"),
    SMS ("Landroid/telephony/SmsManager", "SmsManager"),
    SMS_GSM ("Landroid/telephony/gsm/SmsManager", "GsmSmsManager"),
    LOCATION_LISTENER ("Landroid/location/LocationListener", "LocationListener"),
    GPS_LISTENER ("Landroid/location/GpsStatus$Listener", "GpsStatusListener"),
    GPS_NMEA_LISTENER ("Landroid/location/GpsStatus$NmeaListener", "GpsNmeaListener"),        
    UNKNOWN((String)null, "NULL");

    private TypeReference tRef;
    private final TypeName type;
    private final String prettyName;
    
    AndroidComponent(TypeReference type, final String prettyName) {
        this.tRef = type;
        this.type = type.getName();
        this.prettyName = prettyName;
    }
    
    AndroidComponent(final String type, final String prettyName) {
        if (prettyName.contains(".") || prettyName.contains("/") || prettyName.contains("$") ||
                prettyName.contains(" ") || prettyName.contains("\t")) {
            throw new IllegalArgumentException("The prettyName may not contain one of the reserved characters " +
                    "., /, $ or whitespace. The given name was " + prettyName);
        }
        this.tRef = null;
        this.prettyName = prettyName;
        if (type != null) {
            this.type = TypeName.findOrCreate(type); 
        } else {
            this.type = null;
        }
    }

    public static boolean isAndroidComponent(final TypeReference T, final IClassHierarchy cha) {
        for (final AndroidComponent candid : AndroidComponent.values()) {
            if (candid == AndroidComponent.UNKNOWN) continue;
            if (candid.getName().equals(T.getName())) {
                return true;
            }

            final IClass iT = cha.lookupClass(T);
            final IClass iCand = cha.lookupClass(candid.toReference());
            if (iT == null) {
                return false; // TODO
            }
            if (iCand == null) {
                return false; // ???!!!
            }
            if (cha.isAssignableFrom(iCand, iT)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  A name usable for display-output.
     *
     *  The name returned by this function may not necessarily as 'basename' for the class.
     */
    public String getPrettyName() {
        return this.prettyName;
    }

    /**
     *  The TypeName associated to the component.
     */
    public TypeName getName() {
        return this.type;
    }

    /**
     *  Generates a TypeReference for the component.
     */
    /* package private */ TypeReference toReference() {
        if (this.tRef != null ) {
            return this.tRef;
        } else if (this.type == null) {
            return null;
        } else {
            this.tRef = TypeReference.find(ClassLoaderReference.Primordial, this.type);
            if (this.tRef == null) {
                /* { DEBUG
                    System.out.println("AndroidComponent WARNING: TypeReference.find did not resolve " + this.type.toString());
                } // */
                this.tRef = TypeReference.findOrCreate(ClassLoaderReference.Primordial, this.type);
            }
            if (this.tRef == null) {
                throw new IllegalStateException("Unable to resolve " + this.type.toString() + " in Primordial-Loader. " +
                        "Perhaps the Android-Stubs used are the wrong ones: The analysis needs a view more functions, " +
                        "than the Vanilla-Stubs offer. A script 'stubsBuilder.sh' should have been bundled to generate " +
                        "these.");
            }
            return this.tRef;
        }
    }

    /**
     *  Returns the Element the type matches exactly the given type.
     *
     *  @return The Element if found or AndroidComponent.UNKNOWN if not
     */
    public static AndroidComponent explicit(final TypeName type) {
        for (AndroidComponent test: AndroidComponent.values()) {
            if (type.equals(test.type)) {
                return test;
            }
        }
        return UNKNOWN;
    }

    /**
     *  Returns the Element the type matches exactly the given type.
     *
     *  @return The Element if found or AndroidComponent.UNKNOWN if not
     */
    public static AndroidComponent explicit(final TypeReference type) {
        return explicit(type.getName());
    }

    /**
     *  Returns the Element the type matches exactly the given type.
     *
     *  @return The Element if found or AndroidComponent.UNKNOWN if not
     */
    public static AndroidComponent explicit(String type) {
        if (!(type.startsWith("L") || type.contains("/"))) {
            type = StringStuff.deployment2CanonicalTypeString(type);
        }
        return explicit(TypeName.findOrCreate(type));
    }

    /**
     *  Return the Item that is a matching superclass.
     *
     *  For example returns AndroidComponent.ACTIVITY for 'LauncherActivity'
     *
     *  @return the corresponding Enum-Element or AndroidComponent.UNKNOWN
     */
    public static AndroidComponent from(final IClass type, final IClassHierarchy cha) {
        for (AndroidComponent test: AndroidComponent.values()) {
            if (    (type.getReference().equals(test.toReference())) ||
                    (cha.isSubclassOf(type, cha.lookupClass(test.toReference())))) {
                return test;
            }
        }
        return UNKNOWN;
    }

    /**
     *  Returns the AndroidComponent the method is declared in.
     *
     *  @return the corresponding Enum-Element or AndroidComponent.UNKNOWN
     */
    public static AndroidComponent from(final IMethod method, final IClassHierarchy cha) {
        if (method == null) return AndroidComponent.UNKNOWN;
        IClass type = method.getDeclaringClass();

        if (type == null) {
            throw new IllegalStateException("Unable to retreive the declaring class of " + method);
        }

        for (AndroidComponent test: AndroidComponent.values()) {
            if (test.equals(AndroidComponent.UNKNOWN)) continue;
            final TypeReference testRef = test.toReference();
            if (testRef == null) {
                continue; // Happens when the Android-Stubs are to old
            }
            final IClass testClass = cha.lookupClass(testRef);
            if (testClass == null) {
                continue; // Happens when the Android-Stubs are to old
            }

            if (testClass.isInterface()) {
                if (cha.isAssignableFrom(testClass, type)) {
                    if (testClass.getMethod(method.getSelector()) != null) {
                        return test;
                    }
                }
            } else {
                if (cha.isSubclassOf(type, testClass)) {
                    if (testClass.getMethod(method.getSelector()) != null) {
                        return test;
                    } 
                }
            }
        }
       
        return UNKNOWN; 
    }
}


