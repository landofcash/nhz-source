@ECHO OFF
IF EXIST java (
	start "NHZ NRS" java -cp nhz.jar;lib\*;conf nhz.Nhz
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\jre7" (
		start "NHZ NRS" "%PROGRAMFILES%\Java\jre7\bin\java.exe" -cp nhz.jar;lib\*;conf nhz.Nhz
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Java\jre7" (
			start "NHZ NRS" "%PROGRAMFILES(X86)%\Java\jre7\bin\java.exe" -cp nhz.jar;lib\*;conf nhz.Nhz
		) ELSE (
			ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
			PAUSE
		)
	)
)

