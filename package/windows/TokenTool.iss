;This file will be executed next to the application bundle image
;I.e. current directory will contain folder TokenTool with application files
[Setup]
AppId={{net.rptools.tokentool}}
AppName=TokenTool
AppVersion=2.1
AppVerName=TokenTool 2.1
AppPublisher=Nerps
AppComments=TokenTool by Nerps
AppCopyright=Copyright (C) 2018
AppPublisherURL=http://tokentool.nerps.net/
AppSupportURL=http://forums.rptools.net/viewtopic.php?f=60&t=23681
;AppUpdatesURL=http://java.com/
DefaultDirName={localappdata}/TokenTool
DisableStartupPrompt=Yes
DisableDirPage=No
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=no
DefaultGroupName=Nerps
;Optional License
LicenseFile=COPYING.AFFERO
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=TokenTool-2.1
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=D:\Development\git\JamzTheMan\TokenTool/package/windows/TokenTool.ico
UninstallDisplayIcon={app}/TokenTool.ico
UninstallDisplayName=TokenTool
WizardImageStretch=Yes
WizardSmallImageFile=TokenTool-setup-icon.bmp
WizardImageFile=D:\Development\git\JamzTheMan\TokenTool/package/windows/TokenTool-setup.bmp
ArchitecturesInstallIn64BitMode=x64


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "TokenTool/TokenTool.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "TokenTool/*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
;Source: D:\Development\git\JamzTheMan\TokenTool/package/windows/msvcr100.dll; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\TokenTool"; Filename: "{app}/TokenTool.exe"; IconFilename: "{app}/TokenTool.ico"; Check: returnTrue()
Name: "{commondesktop}\TokenTool"; Filename: "{app}/TokenTool.exe";  IconFilename: "{app}/TokenTool.ico"; Check: returnTrue()


[Run]
Filename: "{app}/TokenTool.exe"; Parameters: "-Xappcds:generatecache"; Check: returnFalse()
Filename: "{app}/TokenTool.exe"; Description: "{cm:LaunchProgram,TokenTool}"; Flags: nowait postinstall skipifsilent; Check: returnTrue()
Filename: "{app}/TokenTool.exe"; Parameters: "-install -svcName ""TokenTool"" -svcDesc ""TokenTool"" -mainExe ""TokenTool.exe""  "; Check: returnFalse()

[UninstallRun]
Filename: "{app}/TokenTool.exe "; Parameters: "-uninstall -svcName TokenTool -stopOnUninstall"; Check: returnFalse()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
