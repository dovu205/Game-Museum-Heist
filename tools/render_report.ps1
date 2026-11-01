param([string]$DocumentPath,[string]$PdfPath)
$ErrorActionPreference='Stop'
$documentFile=(Resolve-Path -LiteralPath $DocumentPath).Path
$pdfFile=[IO.Path]::GetFullPath($PdfPath)
$word=$null
$document=$null
try {
 $word=New-Object -ComObject Word.Application
 $word.Visible=$false
 $word.DisplayAlerts=0
 $word.AutomationSecurity=3
 $document=$word.Documents.Open($documentFile,$false,$false)
 $document.Repaginate()
 $document.Fields.Update() | Out-Null
 foreach($toc in $document.TablesOfContents){$toc.Update()}
 $document.Repaginate()
 foreach($toc in $document.TablesOfContents){$toc.UpdatePageNumbers()}
 $document.Save()
 $document.ExportAsFixedFormat($pdfFile,17)
 Write-Output ('Rendered pages: '+$document.ComputeStatistics(2))
 Write-Output $pdfFile
} finally {
 if($null -ne $document){$document.Close(0);[Runtime.InteropServices.Marshal]::FinalReleaseComObject($document) | Out-Null}
 if($null -ne $word){
  if($word.Documents.Count -eq 0){$word.Quit()}
  [Runtime.InteropServices.Marshal]::FinalReleaseComObject($word) | Out-Null
 }
}
