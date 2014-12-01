
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

installdebug:
	adb -d install -r bin/Pro/SmartRingController-debug.apk

uninstall:
	adb uninstall com.firebirdberlin.smartringcontrollerpro

clean:
	ant clean
	ant clean -f lite.build.xml
