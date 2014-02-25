
all:
	ant release

debug:
	ant debug

lite:
	./genlitesrc.sh
	ant release -f lite.build.xml && adb -d install -r bin/Lite/SmartRingController-release.apk

debuglite:
	./genlitesrc.sh
	ant debug -f lite.build.xml && adb -d install -r bin/Lite/SmartRingController-debug.apk

install:
	adb -d install -r bin/Pro/SmartRingController-release.apk
	adb -d install -r bin/Lite/SmartRingController-release.apk

installdebug:
	adb -d install -r bin/Pro/SmartRingController-debug.apk
	adb -d install -r bin/Lite/SmartRingController-debug.apk


uninstall:
	adb uninstall com.firebirdberlin.smartringcontrollerpro
	adb uninstall com.firebirdberlin.smartringcontrollerlite

clean:
	ant clean
	ant clean -f lite.build.xml
