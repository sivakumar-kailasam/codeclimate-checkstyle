#!/usr/bin/env groovy
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.util.FileNameFinder
import groovy.util.XmlParser

def appContext = setupContext(args)
def includePaths = new JsonSlurper().parse(new File(appContext.configFile), "UTF-8").include_paths?.join(" ")
def codeFolder = new File(appContext.codeFolder)

def filesToAnalyse = new FileNameFinder().getFileNames(appContext.codeFolder, includePaths)

def i = filesToAnalyse.iterator()
while(i.hasNext()) {
    string = i.next()
    if( !string.endsWith(".java")) {
        i.remove()
    }
}

filesToAnalyse = filesToAnalyse.join(" ")
if (filesToAnalyse.isEmpty()) {
    System.exit(0)
}

def sout = new StringBuffer()
def serr = new StringBuffer()

def outputFilePath = "/tmp/analysis.xml"

def analysis = "java -jar /usr/src/app/bin/checkstyle.jar -c /usr/src/app/config/codeclimate_checkstyle.xml -f xml -o ${outputFilePath} ${filesToAnalyse}".execute()
analysis.consumeProcessOutput(sout, serr)
analysis.waitFor()
if (analysis.exitValue() !=0 ) {
	System.err << serr.toString()
}

def outputFile = new File(outputFilePath)
def analysisResult = new XmlParser().parseText(outputFile.text)

analysisResult.file.findAll { file ->
	file.error.findAll { errTag ->
		def defect = JsonOutput.toJson([
			type: "issue",
		       	check_name: cleanupCheckName(errTag.@source),
		       	description: errTag.@message,
		       	categories: [ "Style" ],
		       	location: [
		       		path: file.@name.replaceAll("/code/",""),
		       		positions: [
		       			begin: [
		       				line: errTag.@line.toInteger(),
		       				column: errTag.@column ? errTag.@column.toInteger() : 1,
		       			],
		       			end: [
		       				line: errTag.@line.toInteger(),
		       				column: errTag.@column ? errTag.@column.toInteger() : 1,
		       			]
		       		]
		       ],
					 content: [
					 		body: errTag.@description,
					 ],
					 remediation_points: 150000,
		])
		println "${defect}\0"
	}
}

outputFile.delete()

def setupContext(cmdArgs) {
	def cli = new CliBuilder(usage:"${this.class.name}")
	cli._(longOpt: "configFile", required: true, args: 1, "Path to configuration json file")
	cli._(longOpt: "codeFolder", required: true, args: 1, "Path to code folder")
	cli.parse(cmdArgs)
}

def cleanupCheckName(checkName) {
	checkName.tokenize(".")[-1].split("(?=[A-Z])").join(" ")
}
