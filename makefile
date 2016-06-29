
all:
	ant release

debug:
	ant debug

install:
	adb $(OPT) install -r bin/Pro/SmartRingController-release.apk

installdebug:
	adb $(OPT) install -r bin/Pro/SmartRingController-debug.apk

uninstall:
	adb uninstall com.firebirdberlin.smartringcontrollerpro

clean:
	ant clean
