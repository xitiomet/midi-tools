;NSIS Modern User Interface
;Basic Example Script
;Written by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"

;--------------------------------
;General

  ;Name and file
  Name "MidiTools"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\MidiTools"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\MidiTools" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin
  
  !define REG_UNINSTALL "Software\Microsoft\Windows\CurrentVersion\Uninstall\MidiTools"

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "MidiTools" Main
SectionIn RO
  WriteRegStr HKLM "${REG_UNINSTALL}" "DisplayName" "Midi Tools"
  WriteRegStr HKLM "${REG_UNINSTALL}" "DisplayIcon" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "${REG_UNINSTALL}" "DisplayVersion" "1.0"
  WriteRegStr HKLM "${REG_UNINSTALL}" "Publisher" "openstatic.org"
  WriteRegStr HKLM "${REG_UNINSTALL}" "InstallSource" "$EXEDIR\"
 
  ;Under WinXP this creates two separate buttons: "Modify" and "Remove".
  ;"Modify" will run installer and "Remove" will run uninstaller.
  WriteRegDWord HKLM "${REG_UNINSTALL}" "NoModify" 1
  WriteRegDWord HKLM "${REG_UNINSTALL}" "NoRepair" 0
  WriteRegStr HKLM "${REG_UNINSTALL}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  
  SetOutPath "$INSTDIR"
  
  File ${PROJECT_BUILD_DIR}\MidiTools.exe
  CreateShortcut "$SMPROGRAMS\MidiTools.lnk" "$INSTDIR\MidiTools.exe"

  ;Store installation folder
  WriteRegStr HKCU "Software\MidiTools" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

SectionEnd

Section "jInput Library" jInput

  SetOutPath "$INSTDIR\natives"
  
  File ${PROJECT_BUILD_DIR}\natives\jinput-dx8_64.dll
  File ${PROJECT_BUILD_DIR}\natives\jinput-raw_64.dll
  File ${PROJECT_BUILD_DIR}\natives\jinput-wintab.dll
  
SectionEnd


Section "Java Runtime Environment" java

  SetOutPath "$INSTDIR\jre"
  File /r "${PROJECT_BASEDIR}\jre\*"
SectionEnd

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ;ADD YOUR OWN FILES HERE...

  Delete "$INSTDIR\Uninstall.exe"
  Delete "$INSTDIR\MidiTools.exe"
  Delete "$SMPROGRAMS\MidiTools.lnk"
  RMDir /r "$INSTDIR"

  DeleteRegKey /ifempty HKCU "Software\MidiTools"
  DeleteRegKey HKLM "${REG_UNINSTALL}"
SectionEnd
