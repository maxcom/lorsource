/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/* *******************************************
// LICENSE INFORMATION
// The code, "Detecting Smartphones Using PHP" 
// by Anthony Hand, is licensed under a Creative Commons 
// Attribution 3.0 United States License.
//
// Updated 4 April 2009 by A Hand
//   - Changed the name of the class from DetectSmartPhone to UAgentInfo.
//	   New name is more consistent with PHP and JavaScript classes.
//   - Added a method to detect Opera Mobile and Mini. Opera Mobile is new.
//   - Updated the algorithm for detecting a Sony Mylo.
//	 - Added Android to the DetectTierIphone method.
//   - Updated comments for Detect Tier methods. 
//
// Updated 22 January 2009
//   - Ported to Java from the PHP code by 
//     Satish Kumar Nookala, javasatish@yahoo.com
//
// Anthony Hand, ahand@hand-interactive.com
// Web: www.hand-interactive.com
// 
// License info: http://creativecommons.org/licenses/by/3.0/us/
//
// This code is provided "AS IS" with no expressed or implied warranty.  
// You have the right to use this code or any portion of it 
// so long as you provide credit toward myself as the original author.
//
// *******************************************
*/
package com.handinteractive.mobile;



/**
 * The DetectSmartPhone class encapsulates information about
 *   a browser's connection to your web site. 
 *   You can use it to find out whether the browser asking for
 *   your site's content is probably running on a mobile device.
 *   The methods were written so you can be as granular as you want.
 *   For example, enquiring whether it's as specific as an iPod Touch or
 *   as general as a smartphone class device.
 *   The object's methods return true, or false.
 */
public class UAgentInfo {
    // User-Agent and Accept HTTP request headers
    private String userAgent = "";
    private String httpAccept = "";

    // Initialize some initial smartphone string variables.
    public static final String engineWebKit = "webkit";
    public static final String deviceAndroid = "android";
    public static final String deviceIphone = "iphone";
    public static final String deviceIpod = "ipod";
    public static final String deviceSymbian = "symbian";
    public static final String deviceS60 = "series60";
    public static final String deviceS70 = "series70";
    public static final String deviceS80 = "series80";
    public static final String deviceS90 = "series90";
    public static final String deviceWinMob = "windows ce";
    public static final String deviceWindows = "windows"; 
    public static final String deviceIeMob = "iemobile";
    public static final String enginePie = "wm5 pie"; //An old Windows Mobile
    public static final String deviceBB = "blackberry";   
    public static final String vndRIM = "vnd.rim"; //Detectable when BB devices emulate IE or Firefox
    public static final String devicePalm = "palm";

    public static final String engineBlazer = "blazer"; //Old Palm
    public static final String engineXiino = "xiino"; //Another old Palm

    //Initialize variables for mobile-specific content.
    public static final String vndwap = "vnd.wap";
    public static final String wml = "wml";   

    //Initialize variables for other random devices and mobile browsers.
    public static final String deviceBrew = "brew";
    public static final String deviceDanger = "danger";
    public static final String deviceHiptop = "hiptop";
    public static final String devicePlaystation = "playstation";
    public static final String deviceNintendoDs = "nitro";
    public static final String deviceNintendo = "nintendo";
    public static final String deviceWii = "wii";
    public static final String deviceXbox = "xbox";
    public static final String deviceArchos = "archos";

    public static final String engineOpera = "opera"; //Popular browser
    public static final String engineNetfront = "netfront"; //Common embedded OS browser
    public static final String engineUpBrowser = "up.browser"; //common on some phones
    public static final String engineOpenWeb = "openweb"; //Transcoding by OpenWave server
    public static final String deviceMidp = "midp"; //a mobile Java technology
    public static final String uplink = "up.link";

    public static final String devicePda = "pda"; //some devices report themselves as PDAs
    public static final String mini = "mini";  //Some mobile browsers put "mini" in their names.
    public static final String mobile = "mobile"; //Some mobile browsers put "mobile" in their user agent strings.
    public static final String mobi = "mobi"; //Some mobile browsers put "mobi" in their user agent strings.

    //Use Maemo, Tablet, and Linux to test for Nokia"s Internet Tablets.
    public static final String maemo = "maemo";
    public static final String maemoTablet = "tablet";
    public static final String linux = "linux";
    public static final String qtembedded = "qt embedded"; //for Sony Mylo
    public static final String mylocom2 = "com2"; //for Sony Mylo also

    //In some UserAgents, the only clue is the manufacturer.
    public static final String manuSonyEricsson = "sonyericsson";
    public static final String manuericsson = "ericsson";
    public static final String manuSamsung1 = "sec-sgh";
    public static final String manuSony = "sony";

    //In some UserAgents, the only clue is the operator.
    public static final String svcDocomo = "docomo";
    public static final String svcKddi = "kddi";
    public static final String svcVodafone = "vodafone";

    /**
     * Initialize the userAgent and httpAccept variables
     *
     * @param userAgent the User-Agent header
     * @param httpAccept the Accept header
     */
    public UAgentInfo(String userAgent, String httpAccept) {
        if (userAgent != null) {
            this.userAgent = userAgent.toLowerCase();
        }
        if (httpAccept != null) {
            this.httpAccept = httpAccept.toLowerCase();
        }
    }

    /**
     * Return the lower case HTTP_USER_AGENT
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Return the lower case HTTP_ACCEPT
     */
    public String getHttpAccept() {
        return httpAccept;
    }

    /**
     * Detects if the current device is an iPhone.
     */
    public boolean detectIphone() {
        // The iPod touch says it's an iPhone! So let's disambiguate.
        if (userAgent.contains(deviceIphone) && !detectIpod()) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an iPod Touch.
     */
    public boolean detectIpod() {
        if (userAgent.contains(deviceIpod)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an iPhone or iPod Touch.
     */
    public boolean detectIphoneOrIpod() {
        //We repeat the searches here because some iPods may report themselves as an iPhone, which would be okay.
        if (userAgent.contains(deviceIphone) ||
          userAgent.contains(deviceIpod)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an Android OS-based device.
     */
    public boolean detectAndroid() {
        if (userAgent.contains(deviceAndroid)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an Android OS-based device and
     * the browser is based on WebKit.
     */
    public boolean detectAndroidWebKit() {
        if (detectAndroid() && detectWebkit()) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is based on WebKit.
     */
    public boolean detectWebkit() {
        if (userAgent.contains(engineWebKit)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is the S60 Open Source Browser.
     */
    public boolean detectS60OssBrowser() {
        //First, test for WebKit, then make sure it's either Symbian or S60.
        if (detectWebkit() &&
                (userAgent.contains(deviceSymbian) ||
                  userAgent.contains(deviceS60))) {
            return true;
        }
        return false;
    }

    /**
     *
     * Detects if the current device is any Symbian OS-based device,
     *   including older S60, Series 70, Series 80, Series 90, and UIQ, 
     *   or other browsers running on these devices.
     */
    public boolean detectSymbianOS() {
        if (userAgent.contains(deviceSymbian) ||
          userAgent.contains(deviceS60) ||
          userAgent.contains(deviceS70) ||
          userAgent.contains(deviceS80) ||
          userAgent.contains(deviceS90)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is a Windows Mobile device.
     */
    public boolean detectWindowsMobile() {
        //Most devices use 'Windows CE', but some report 'iemobile' 
        //  and some older ones report as 'PIE' for Pocket IE. 
        if (userAgent.contains(deviceWinMob) ||
          userAgent.contains(deviceIeMob) ||
          userAgent.contains(enginePie) ||
                (detectWapWml() && userAgent.contains(deviceWindows))) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is a BlackBerry of some sort.
     */
    public boolean detectBlackBerry() {
        if (userAgent.contains(deviceBB) || httpAccept.contains(vndRIM)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is on a PalmOS device.
     */
    public boolean detectPalmOS() {
        //Most devices nowadays report as 'Palm', but some older ones reported as Blazer or Xiino.
        if (userAgent.indexOf(devicePalm) != -1 ||
          userAgent.contains(engineBlazer) ||
          userAgent.contains(engineXiino)) {
            return true;
        }
        return false;
    }

    /**
     * Check to see whether the device is any device
     *   in the 'smartphone' category.
     */
    public boolean detectSmartphone() {
        return (detectIphoneOrIpod() ||
                detectS60OssBrowser() ||
                detectSymbianOS() ||
                detectWindowsMobile() ||
                detectBlackBerry() ||
                detectPalmOS() ||
                detectAndroid());
    }

    /**
     * Detects whether the device is a Brew-powered device.
     */
    public boolean detectBrewDevice() {
        if (userAgent.contains(deviceBrew)) {
            return true;
        }
        return false;
    }

    /**
     * Detects the Danger Hiptop device.
     */
    public boolean detectDangerHiptop() {
        if (userAgent.contains(deviceDanger) ||
          userAgent.contains(deviceHiptop)) {
            return true;
        }
        return false;
    }

    /**
     * Detects Opera Mobile or Opera Mini.
     * Added by AHand
     */
    public boolean detectOperaMobile() {
        if (userAgent.contains(engineOpera) &&
                (userAgent.contains(mini) ||
                  userAgent.contains(mobi))) {
            return true;
        }
        return false;
    }

    /**
     * Detects whether the device supports WAP or WML.
     */
    public boolean detectWapWml() {
        if (httpAccept.indexOf(vndwap) != -1 ||
          httpAccept.contains(wml)) {
            return true;
        }
        return false;
    }

    /**
     * The quick way to detect for a mobile device.
     *  Will probably detect most recent/current mid-tier Feature Phones
     *  as well as smartphone-class devices.
     */
    public boolean detectMobileQuick() {
        //Ordered roughly by market share, WAP/XML > Brew > Smartphone.
        if (detectWapWml()) { return true; }
        if (detectBrewDevice()) { return true; }
        
        // Updated by AHand
        if (detectOperaMobile()) { return true; }
        
        if (userAgent.contains(engineUpBrowser)) { return true; }
        if (userAgent.contains(engineOpenWeb)) { return true; }
        if (userAgent.contains(deviceMidp)) { return true; }

        if (detectSmartphone()) { return true; }
        if (detectDangerHiptop()) { return true; }

        if (detectMidpCapable()) { return true; }

        if (userAgent.contains(devicePda)) { return true; }
        if (userAgent.contains(mobile)) { return true; }

        //detect older phones from certain manufacturers and operators.
        if (userAgent.contains(uplink)) { return true; }
        if (userAgent.contains(manuSonyEricsson)) { return true; }
        if (userAgent.contains(manuericsson)) { return true; }
        if (userAgent.contains(manuSamsung1)) { return true; }
        if (userAgent.contains(svcDocomo)) { return true; }
        if (userAgent.contains(svcKddi)) { return true; }
        if (userAgent.contains(svcVodafone)) { return true; }

        return false;
    }

    /**
     * Detects if the current device is a Sony Playstation.
     */
    public boolean detectSonyPlaystation() {
        if (userAgent.contains(devicePlaystation)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is a Nintendo game device.
     */
    public boolean detectNintendo() {
        if (userAgent.contains(deviceNintendo) ||
          userAgent.contains(deviceWii) ||
          userAgent.contains(deviceNintendoDs)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is a Microsoft Xbox.
     */
    public boolean detectXbox() {
        if (userAgent.contains(deviceXbox)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an Internet-capable game console.
     */
    public boolean detectGameConsole() {
        if (detectSonyPlaystation() ||
                detectNintendo() ||
                detectXbox()) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device supports MIDP, a mobile Java technology.
     */
    public boolean detectMidpCapable() {
        if (userAgent.contains(deviceMidp) ||
          httpAccept.contains(deviceMidp)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is on one of the Maemo-based Nokia Internet Tablets.
     */
    public boolean detectMaemoTablet() {
        if (userAgent.contains(maemo)) {
            return true;
        } else if (userAgent.contains(maemoTablet) &&
          userAgent.contains(linux)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current device is an Archos media player/Internet tablet.
     */
    public boolean detectArchos() {
        if (userAgent.contains(deviceArchos)) {
            return true;
        }
        return false;
    }

    /**
     * Detects if the current browser is a Sony Mylo device.
     * Updated by AHand
     */
    public boolean detectSonyMylo() {
        if (userAgent.contains(manuSony) &&
                (userAgent.contains(qtembedded) ||
                  userAgent.contains(mylocom2))) {
            return true;
        }
        return false;
    }

    /**
     * The longer and more thorough way to detect for a mobile device.
     *   Will probably detect most feature phones,
     *   smartphone-class devices, Internet Tablets, 
     *   Internet-enabled game consoles, etc.
     *   This ought to catch a lot of the more obscure and older devices, also --
     *   but no promises on thoroughness!
     */
    public boolean detectMobileLong() {
        if (detectMobileQuick() ||
                detectMaemoTablet() ||
                detectGameConsole()) {
            return true;
        }
        return false;
    }


    //*****************************
    // For Mobile Web Site Design
    //*****************************

    /**
     * The quick way to detect for a tier of devices.
     *   This method detects for devices which can 
     *   display iPhone-optimized web content.
     *   Includes iPhone, iPod Touch, Android, etc.
     */
    public boolean detectTierIphone() {
        if (detectIphoneOrIpod() ||
			detectAndroid() ||
			detectAndroidWebKit()) {
            return true;
        }
        return false;
    }

    /**
     * The quick way to detect for a tier of devices.
     *   This method detects for all smartphones, but
     *   excludes the iPhone Tier devices.
     */
    public boolean detectTierSmartphones() {
        if (detectSmartphone() && (!detectTierIphone())) {
            return true;
        }
        return false;
    }

    /**
     * The quick way to detect for a tier of devices.
     *   This method detects for all other types of phones,
     *   but excludes the iPhone and Smartphone Tier devices. 
     */
    public boolean detectTierOtherPhones() {
        if (detectMobileQuick() && (!detectTierIphone()) && (!detectTierSmartphones())) {
            return true;
        }
        return false;
    }
}
