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

install_updater_app() {
  ui_print "- Installing microG Updater app..."
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

DUMP_GMS="$(pm dump com.google.android.gms 2>/dev/null)"
if [ -n "$DUMP_GMS" ] && ! (echo "$DUMP_GMS" | grep "Unable to find package: com.google.android.gms") >/dev/null; then
    IS_MICROG_INSTALLED=true
fi

# Always install microG Updater app
install_updater_app

if $IS_MICROG_INSTALLED; then
    ui_print "- Existing microG installation detected on device."
    perform_microg_copy
else
    ui_print "*************************************************"
    ui_print " NOTICE: microG is not detected on your device."
    ui_print " Please open the microG Updater app after rebooting"
    ui_print " to download and finish microG installation."
    ui_print "*************************************************"
fi

mmm_exec hideLoading
