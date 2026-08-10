if [ -n "$MMM_EXT_SUPPORT" ]; then
  ui_print "#!useExt"
  mmm_exec() {
    ui_print "$(echo "#!$@")"
  }
else
  mmm_exec() { true; }
fi
if ! $BOOTMODE; then
    abort "- ERROR: Installation via recovery is NOT supported."
fi
mmm_exec setSupportLink "https://github.com/306bobby-android/microg_installer_next/issues"

MAX_VER="252432099"
MAX_VERN="0.3.16.252432"

if [ -f /data/adb/Phonesky.apk ]; then
    ui_print "- INFO: Phonesky.apk is found in /data/adb, but this module no longer uses this file."
    ui_print "- INFO: It won't break anything, but having that there won't make you use real Play Store anymore."
fi

# Helper: Keycheck for Volume Keys
keycheck() {
  local num=""
  while true; do
    num=$(getevent -l -c 1 2>&1 | grep -E 'KEY_VOLUMEUP|KEY_VOLUMEDOWN')
    if echo "$num" | grep -q 'KEY_VOLUMEUP'; then
      return 0
    elif echo "$num" | grep -q 'KEY_VOLUMEDOWN'; then
      return 1
    fi
  done
}

install_updater_app() {
  ui_print "- Installing / Updating microG Updater app..."
  local SRC_APK=""
  if [ -f "$TMPDIR/system/product/priv-app/microGUpdater/microGUpdater.apk" ]; then
    SRC_APK="$TMPDIR/system/product/priv-app/microGUpdater/microGUpdater.apk"
  elif [ -f "$TMPDIR/system/priv-app/microGUpdater/microGUpdater.apk" ]; then
    SRC_APK="$TMPDIR/system/priv-app/microGUpdater/microGUpdater.apk"
  elif [ -f "$MODPATH/system/product/priv-app/microGUpdater/microGUpdater.apk" ]; then
    SRC_APK="$MODPATH/system/product/priv-app/microGUpdater/microGUpdater.apk"
  elif [ -f "$MODPATH/system/priv-app/microGUpdater/microGUpdater.apk" ]; then
    SRC_APK="$MODPATH/system/priv-app/microGUpdater/microGUpdater.apk"
  elif [ -n "$ZIPFILE_PATH" ] && [ -f "$ZIPFILE_PATH/system/priv-app/microGUpdater/microGUpdater.apk" ]; then
    SRC_APK="$ZIPFILE_PATH/system/priv-app/microGUpdater/microGUpdater.apk"
  fi

  if [ ! -d "/my_bigball/priv-app/GmsCore" ]; then
    mkdir -p "$MODPATH/system/product/priv-app/microGUpdater"
    if [ -n "$SRC_APK" ] && [ -f "$SRC_APK" ]; then
      cp -f "$SRC_APK" "$MODPATH/system/product/priv-app/microGUpdater/microGUpdater.apk"
    fi
  else
    mkdir -p "$MODPATH/system/priv-app/microGUpdater"
    if [ -n "$SRC_APK" ] && [ -f "$SRC_APK" ]; then
      cp -f "$SRC_APK" "$MODPATH/system/priv-app/microGUpdater/microGUpdater.apk"
    fi
  fi
}

remove_updater_app() {
  ui_print "- Removing microG Updater app from module."
  rm -rf "$MODPATH/system/priv-app/microGUpdater" "$MODPATH/system/product/priv-app/microGUpdater" 2>/dev/null || true
}

prompt_updater() {
  local prompt_title="$1"
  ui_print "*************************************************"
  ui_print " $prompt_title"
  ui_print "   Vol Up   = Yes (Install microG Updater)"
  ui_print "   Vol Down = No  (Skip microG Updater)"
  ui_print "*************************************************"
  if keycheck; then
    install_updater_app
  else
    remove_updater_app
  fi
}

perform_microg_copy() {
  DUMP_GMS="$(pm dump com.google.android.gms 2>/dev/null)"
  if [ -n "$DUMP_GMS" ] && ! (echo "$DUMP_GMS" | grep "Unable to find package: com.google.android.gms") >/dev/null; then
    GMS_PATH="$(realpath $(echo "$DUMP_GMS" | grep path: | head -n1 | cut -d: -f2) 2>/dev/null)"
    if [ -n "$GMS_PATH" ] && [ -f "$GMS_PATH" ]; then
      ui_print "- Copying installed microG GmsCore to system priv-app"
      if [ ! -d "/my_bigball/priv-app/GmsCore" ]; then
        mkdir -p "$MODPATH/system/product/priv-app/GmsCore"
        cp "$GMS_PATH" "$MODPATH/system/product/priv-app/GmsCore/GmsCore.apk"
      else
        mkdir -p "$MODPATH/system/priv-app/microG"
        cp "$GMS_PATH" "$MODPATH/system/priv-app/microG/microG.apk"
      fi
    fi
  fi

  DUMP_VD="$(pm dump com.android.vending 2>/dev/null)"
  if [ -n "$DUMP_VD" ] && ! (echo "$DUMP_VD" | grep "Unable to find package: com.android.vending") >/dev/null; then
    VD_PATH="$(realpath $(echo "$DUMP_VD" | grep path: | head -n1 | cut -d: -f2) 2>/dev/null)"
    if [ -n "$VD_PATH" ] && [ -f "$VD_PATH" ]; then
      if (echo "$DUMP_VD" | grep "android.permission.FAKE_PACKAGE_SIGNATURE") >/dev/null; then
        ui_print "- Copying installed microG Companion to system priv-app"
        pm grant com.android.vending android.permission.FAKE_PACKAGE_SIGNATURE 2>/dev/null
      else
        ui_print "- Copying installed Play Store to system priv-app"
      fi
      if ! [ -d "/my_bigball/priv-app/GmsCore" ]; then
        mkdir -p "$MODPATH/system/product/priv-app/Phonesky"
        cp "$VD_PATH" "$MODPATH/system/product/priv-app/Phonesky/Phonesky.apk"
      else
        mkdir -p "$MODPATH/system/priv-app/Phonesky"
        cp "$VD_PATH" "$MODPATH/system/priv-app/Phonesky/Phonesky.apk"
      fi
    fi
  fi
}

# -------------------------------------------------------------
# Detection phase
# -------------------------------------------------------------
IS_MICROG_INSTALLED=false
IS_OUR_MODULE_INSTALLED=false
IS_OLD_REVIVED_INSTALLED=false
IS_UPDATER_INSTALLED=false

DUMP_GMS="$(pm dump com.google.android.gms 2>/dev/null)"
if [ -n "$DUMP_GMS" ] && ! (echo "$DUMP_GMS" | grep "Unable to find package: com.google.android.gms") >/dev/null; then
    IS_MICROG_INSTALLED=true
fi

PREV_PROP=""
if [ -f "/data/adb/modules/microg_installer/module.prop" ]; then
    PREV_PROP="$(cat /data/adb/modules/microg_installer/module.prop 2>/dev/null)"
elif [ -n "$NVBASE" ] && [ -f "$NVBASE/modules/microg_installer/module.prop" ]; then
    PREV_PROP="$(cat "$NVBASE/modules/microg_installer/module.prop" 2>/dev/null)"
fi

if [ -n "$PREV_PROP" ]; then
    if echo "$PREV_PROP" | grep -E "microG Installer Next|306bobby-android" >/dev/null; then
        IS_OUR_MODULE_INSTALLED=true
    elif echo "$PREV_PROP" | grep -E "microG Installer Revived|nift4" >/dev/null; then
        IS_OLD_REVIVED_INSTALLED=true
    fi
fi

if pm path org.microg.installer.updater >/dev/null 2>&1 || \
   [ -f "/data/adb/modules/microg_installer/system/priv-app/microGUpdater/microGUpdater.apk" ] || \
   [ -f "/data/adb/modules/microg_installer/system/product/priv-app/microGUpdater/microGUpdater.apk" ]; then
    IS_UPDATER_INSTALLED=true
fi

# -------------------------------------------------------------
# Flow Execution
# -------------------------------------------------------------
if $IS_OUR_MODULE_INSTALLED; then
    ui_print "- Existing microG Installer Next module detected."
    perform_microg_copy
    if $IS_UPDATER_INSTALLED; then
        ui_print "- microG Updater app is already installed. Module updated."
        install_updater_app
    else
        prompt_updater "Install microG Updater app to receive updates automatically?"
    fi

elif $IS_OLD_REVIVED_INSTALLED; then
    ui_print "- Legacy microG Installer Revived module detected."
    ui_print "- Replacing old module with microG Installer Next."
    perform_microg_copy
    prompt_updater "Install microG Updater app to continue receiving updates automatically?"

elif $IS_MICROG_INSTALLED; then
    ui_print "- Existing microG installation detected on device."
    perform_microg_copy
    prompt_updater "microG already detected! Install Updater app to receive updates automatically?"

else
    ui_print "- Clean installation detected (microG not installed)."
    ui_print "- Note: The microG Updater app is required to update microG."
    perform_microg_copy
    prompt_updater "Install microG Updater app? (Required to update microG)"
fi

mmm_exec hideLoading
