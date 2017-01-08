for %%j in (%*) do (
	java -jar MakeApkMovable.jar "%%~j"
)

pause