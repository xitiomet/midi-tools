;NSIS Modern User Interface
;Basic Example Script
;Written by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"

;--------------------------------

 
!ifndef FileAssociation_INCLUDED
!define FileAssociation_INCLUDED
 
!include Util.nsh
 
!verbose push
!verbose 3
!ifndef _FileAssociation_VERBOSE
  !define _FileAssociation_VERBOSE 3
!endif
!verbose ${_FileAssociation_VERBOSE}
!define FileAssociation_VERBOSE `!insertmacro FileAssociation_VERBOSE`
!verbose pop
 
!macro FileAssociation_VERBOSE _VERBOSE
  !verbose push
  !verbose 3
  !undef _FileAssociation_VERBOSE
  !define _FileAssociation_VERBOSE ${_VERBOSE}
  !verbose pop
!macroend
 
 
 
!macro RegisterExtensionCall _EXECUTABLE _EXTENSION _DESCRIPTION
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
  Push `${_DESCRIPTION}`
  Push `${_EXTENSION}`
  Push `${_EXECUTABLE}`
  ${CallArtificialFunction} RegisterExtension_
  !verbose pop
!macroend
 
!macro UnRegisterExtensionCall _EXTENSION _DESCRIPTION
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
  Push `${_EXTENSION}`
  Push `${_DESCRIPTION}`
  ${CallArtificialFunction} UnRegisterExtension_
  !verbose pop
!macroend
 
 
 
!define RegisterExtension `!insertmacro RegisterExtensionCall`
!define un.RegisterExtension `!insertmacro RegisterExtensionCall`
 
!macro RegisterExtension
!macroend
 
!macro un.RegisterExtension
!macroend
 
!macro RegisterExtension_
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
 
  Exch $R2 ;exe
  Exch
  Exch $R1 ;ext
  Exch
  Exch 2
  Exch $R0 ;desc
  Exch 2
  Push $0
  Push $1
 
  ReadRegStr $1 HKCR $R1 ""  ; read current file association
  StrCmp "$1" "" NoBackup  ; is it empty
  StrCmp "$1" "$R0" NoBackup  ; is it our own
    WriteRegStr HKCR $R1 "backup_val" "$1"  ; backup current value
NoBackup:
  WriteRegStr HKCR $R1 "" "$R0"  ; set our file association
 
  ReadRegStr $0 HKCR $R0 ""
  StrCmp $0 "" 0 Skip
    WriteRegStr HKCR "$R0" "" "$R0"
    WriteRegStr HKCR "$R0\shell" "" "open"
    WriteRegStr HKCR "$R0\DefaultIcon" "" "$R2,0"
Skip:
  WriteRegStr HKCR "$R0\shell\open\command" "" '"$R2" "%1"'
  WriteRegStr HKCR "$R0\shell\edit" "" "Edit $R0"
  WriteRegStr HKCR "$R0\shell\edit\command" "" '"$R2" "%1"'
 
  Pop $1
  Pop $0
  Pop $R2
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
 
 
!define UnRegisterExtension `!insertmacro UnRegisterExtensionCall`
!define un.UnRegisterExtension `!insertmacro UnRegisterExtensionCall`
 
!macro UnRegisterExtension
!macroend
 
!macro un.UnRegisterExtension
!macroend
 
!macro UnRegisterExtension_
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
 
  Exch $R1 ;desc
  Exch
  Exch $R0 ;ext
  Exch
  Push $0
  Push $1
 
  ReadRegStr $1 HKCR $R0 ""
  StrCmp $1 $R1 0 NoOwn ; only do this if we own it
  ReadRegStr $1 HKCR $R0 "backup_val"
  StrCmp $1 "" 0 Restore ; if backup="" then delete the whole key
  DeleteRegKey HKCR $R0
  Goto NoOwn
 
Restore:
  WriteRegStr HKCR $R0 "" $1
  DeleteRegValue HKCR $R0 "backup_val"
  DeleteRegKey HKCR $R1 ;Delete key with association name settings
 
NoOwn:
 
  Pop $1
  Pop $0
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
!endif # !FileAssociation_INCLUDED

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
  
  ${registerExtension} "$INSTDIR\MidiTools.exe" ".mtz" "Midi Tools Project"

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
  ${unregisterExtension} ".mtz" "Midi Tools Project"
  DeleteRegKey /ifempty HKCU "Software\MidiTools"
  DeleteRegKey HKLM "${REG_UNINSTALL}"
SectionEnd
