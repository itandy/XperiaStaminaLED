package com.gzplanet.xposed.xperiastaminaled;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XperiaStaminaLed implements IXposedHookLoadPackage {
	private static final String CLASS_XSSM = "com.sonymobile.superstamina.xssm.Xssm";
	private static final String CLASS_POWER_STATE = "com.sonymobile.superstamina.xssm.PowerState";
	private static final String CLASS_POWER_SERVICE = "com.sonymobile.superstamina.XperiaPowerService";

	private final static String PKGNAME_STAMINA = "com.sonymobile.superstamina";
	
	private final static String METHODNAME_2013 = "enableLedsOverride";
	private final static String METHODNAME_2012 = "enableLeds";

	Class<? extends Enum> mPowerStateEnum;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(PKGNAME_STAMINA))
			return;

		/* determine phone model and method to use */

		// check for 2012 model method (e.g. P, U...)
		boolean use2013Method;
		try {
			XposedHelpers.findMethodExact(CLASS_POWER_SERVICE, lpparam.classLoader, METHODNAME_2012, boolean.class);
			use2013Method = false;
		} catch (NoSuchMethodError e1) {
			// check for 2013 model method (e.g. Z, ZR...)
			try {
				XposedHelpers.findMethodExact(CLASS_POWER_SERVICE, lpparam.classLoader, METHODNAME_2013,
						boolean.class);
				use2013Method = true;
			} catch (NoSuchMethodError e2) {
				XposedBridge.log("No supported methods found");
				return;
			}
		}

		if (use2013Method) {
			mPowerStateEnum = (Class<? extends Enum>) XposedHelpers.findClass(CLASS_POWER_STATE, lpparam.classLoader);
			try {
				XposedHelpers.findMethodExact(CLASS_XSSM, lpparam.classLoader, "setPowerStateLocked", mPowerStateEnum);

				XposedBridge.log("enableLedsOverride found, setPowerStateLocked found");

				XposedHelpers.findAndHookMethod(CLASS_XSSM, lpparam.classLoader, "setPowerStateLocked",
						mPowerStateEnum, new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
								int powerState = -1;
								if (param.args[0] == Enum.valueOf(mPowerStateEnum, "POWER_STATE_0"))
									powerState = 0;
								else if (param.args[0] == Enum.valueOf(mPowerStateEnum, "POWER_STATE_1"))
									powerState = 1;
								else if (param.args[0] == Enum.valueOf(mPowerStateEnum, "POWER_STATE_2"))
									powerState = 2;
								XposedBridge.log(String.format("POWER_STATE_%d", powerState));

								if (param.args[0] == Enum.valueOf(mPowerStateEnum, "POWER_STATE_1")) {
									XposedHelpers.callMethod(
											XposedHelpers.getObjectField(param.thisObject, "mService"),
											METHODNAME_2013, false);
									XposedBridge.log("Invoked enableLedsOverride(false)");
								}
							}
						});
			} catch (NoSuchMethodError e3) {
				XposedBridge.log("enableLedsOverride found, setPowerStateLocked not found");
			}

		} else {
			try {
				XposedHelpers.findMethodExact(CLASS_XSSM, lpparam.classLoader, "activateImpl", boolean.class);

				XposedBridge.log("enableLeds found, activateImpl found");

				XposedHelpers.findAndHookMethod(CLASS_XSSM, lpparam.classLoader, "activateImpl", boolean.class,
						new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
								boolean param1 = (Boolean) param.args[0];
								XposedBridge.log(String.format("Parameter 1: %b", param1));

								if (param1) {
									XposedHelpers.callMethod(
											XposedHelpers.getObjectField(param.thisObject, "mService"), METHODNAME_2012,
											true);
									XposedBridge.log("Invoked enableLeds(true)");
								}
							}
						});
			} catch (NoSuchMethodError e4) {
				XposedBridge.log("enableLeds found, activateImpl not found");
			}
		}
	}
}
