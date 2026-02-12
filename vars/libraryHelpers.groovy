import groovy.json.JsonSlurper

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

/**
 * Extracts the target environment ID from the given Jenkins job name.
 * For example, a job name like 'Deploy_D35_FAST' will extract 'D35' as the environment ID.
 *
 * @param jobName The name of the Jenkins job.
 * @return The extracted environment ID, or null if the job name is invalid.
 */
def extractTargetEnvFromJobname ( jobName ) {
    def matcher = (jobName =~ /(?i)((?<=_)(Train|Q|ProJob|Prod|Pro|P|D)[0-9]{0,5}(_[0-9]{1,5})?)/)
    try {
        String envId = matcher[0][1]
        println "extracted envId '${envId}' from jobname '${jobName}'"
        return envId
    } catch (Exception e) {
        println 'Job name is wrong. Cannot get System Number'
    }
}

/**
 * Resolves the target environment to be used in the pipeline script.
 * If a target environment is provided, it is used directly. Otherwise, it is extracted from the job name.
 *
 * @param jobName The name of the Jenkins job.
 * @param providedTargetEnv The target environment provided explicitly (optional).
 * @return The resolved target environment.
 */
def resolveTargetEnv ( jobName, providedTargetEnv ) {
    def resolvedTargetEnv = ''

    if( providedTargetEnv == null || providedTargetEnv.isEmpty() ) {
        resolvedTargetEnv = extractTargetEnvFromJobname(jobName)
    } else {
        println "targetEnv ${providedTargetEnv} was provided"
        resolvedTargetEnv = providedTargetEnv
    }
    println "resolving targetEnv=${resolvedTargetEnv}"
    return resolvedTargetEnv
}

def String extractRepoNameFromGitRepoUrl(String gitRepoUrl) {
    def m = gitRepoUrl =~ /\/([^\/]+?)(?:\.git)?\/?$/
    def repositoryName = m ? m[0][1] : null
    println "extracted repositoryName '${repositoryName}' from git URL '${gitRepoUrl}'"
    return repositoryName
}

/**
 * Returns the value associated with the key in the provided parameters map
 * or returns the default value if the key is not found or the associated value is null.
 * In case the value is a String the default value is also returned in case the value is an empty String.
 *
 * @param params A map containing key-value pairs.
 * @param key The key to look up in the map.
 * @param defaultValue default value that should be returned
 * @return The value associated with the key, or the default value.
 */
def getOrDefaultString (Map params, key, defaultValue ) {
    def value = params.getOrDefault(key, defaultValue)
    println "retrieved value='${value}' for key='${key}' from params"
    return getOrDefaultString(value, defaultValue)
}

/**
 * Returns the provided value if it is not null or in case of a String empty; otherwise, returns the default value.
 *
 * @param value The value to check.
 * @param defaultValue The default value to return if the provided value is null or in case of String empty.
 * @return The provided value or the default value.
 */
def getOrDefaultString (value, defaultValue ) {
    if(value == null || (value instanceof String && value.isEmpty())) {
        println "using default value=${defaultValue}"
        return defaultValue
    }
    println "using provided value=${value}"
    return value
}

def String appendIfNotEmpty(String base, String toCheck, String toAppend){
    if(toCheck != null && !toCheck.isEmpty()){
        return base + toAppend
    }
    return base
}

def String appendIfTrue(String base, boolean toCheck, String toAppend){
    if(toCheck){
        return base + toAppend
    }
    return base
}

/**
 * Sets up the Tomcat environment variables for the specified application and instance.
 * Reads the configuration from a YAML file and assigns the values to the environment variables.
 *
 * @param applicationName The name of the application.
 * @param instanceName The name of the instance.
 * @param env The environment object where the variables will be set.
 * @throws Exception If the application or instance is not found in the configuration file.
 */
def setupTomcatEnv( applicationName, instanceName, env ) {
    println "reading tomcat config for ${applicationName}/${instanceName}"
    def tomcatYaml = readYaml file: '.\\pipelines\\configs\\tomcats\\tomcats.yaml'

    if(!tomcatYaml[applicationName]) {
        throw new Exception("Can't find application ${applicationName} in tomcats.yaml")
    } else if(!tomcatYaml[applicationName][instanceName]){
        throw new Exception("Can't find instance with name ${instanceName} in application ${applicationName} in tomcats.yaml")
    }

    env.APACHE_PATH         = tomcatYaml[applicationName][instanceName]['path'] // folder name of tomcat, not fully qualified path
    env.APACHE_SERVICE_NAME = tomcatYaml[applicationName][instanceName]['service'] // windows service name of tomcat
    env.APACHE_HOST         = tomcatYaml[applicationName][instanceName]['host'] // hostname on which tomcat is running
    env.APACHE_PORT         = tomcatYaml[applicationName][instanceName]['port']
    env.PROFILE_NAME        = tomcatYaml[applicationName][instanceName]['profile'] // dev, prod, q etc
    env.IS_SSL_ON           = tomcatYaml[applicationName][instanceName]['ssl']
    env.JKS_STORAGE_PATH    = tomcatYaml[applicationName][instanceName]['jks_storage_path']
    env.ARTIFACT_NAME       = tomcatYaml[applicationName][instanceName]['artifact_name']
    env.LOG_PATH            = tomcatYaml[applicationName][instanceName]['log_path']
    env.FW_RULE_NAME        = tomcatYaml[applicationName][instanceName]['firewall_rule']
    env.LOG_STASH_HOST      = tomcatYaml[applicationName][instanceName]["LOG_STASH_HOST"]
    env.LOG_STASH_PORT      = tomcatYaml[applicationName][instanceName]["LOG_STASH_PORT"]
    env.INSTANCE_ID         = tomcatYaml[applicationName][instanceName]["INSTANCE_ID"]

    println "Found values: tomcat folder=${env.APACHE_PATH}, service name=${env.APACHE_SERVICE_NAME}, host=${env.APACHE_HOST}, port=${env.APACHE_PORT}"
    printEnvironmentConfiguration(env)

}

/**
 * Writes build properties to a specified file.

 * @param buildPropertiesFile The path to the file where the build properties will be written. Existing file will be overwritten.
 * @param gitBranch The name of the Git branch.
 * @param gitCommit The Git commit hash.
 * @param buildNumber The build number.
 * @param jobUrl The URL of the Jenkins job.
 * @param targetEnv The target environment for the build.
 * @param buildProfile (Optional) The build profile.
 * @param springProfile (Optional) The Spring profile.
 */
def writeBuildProperties ( buildPropertiesFile, gitBranch, gitCommit, buildNumber, jobUrl, targetEnv, buildProfile='', springProfile='' ) {
    println "Writing build properties to ${buildPropertiesFile}"
    def currentDateTime = new Date().format("dd.MM.yyyy@HH-mm-ss")
    def buildPropertiesContent = """
        git_branch=${gitBranch}
        git_commit=${gitCommit}
        job_id=${buildNumber}
        job_url=${jobUrl}
        build_id=${currentDateTime}
        spring.profiles.active.generated=${springProfile}
        buildProfile=${buildProfile}
        targetEnv=${targetEnv}
    """
    writeFile(file: buildPropertiesFile, text: buildPropertiesContent, encoding: "UTF-8")
    println "Written build properties ${buildPropertiesContent}"
}

def checkoutPurpdmRepo ( gitBranch ) {
    return checkoutRepository(
        'https://sourcecode.socialcoding.bosch.com/scm/purchasing_it/purpdm.git',
        gitBranch,
        [[$class: 'PerBuildTag']]
    )
}

def checkoutJenkinsPipelineRepo ( ansibleScriptsBranch ) {
    return checkoutRepository(
        'https://sourcecode.socialcoding.bosch.com/scm/purchasing_it/purpdm-jenkins-pipeline.git',
        ansibleScriptsBranch,
        [[$class: 'LocalBranch', localBranch: "**"], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'pipelines']]
    )
}

def checkoutRepository ( gitUrl, gitBranch, extensions = [] ) {
    println "Checking out branch ${gitBranch} on repository ${gitUrl}"
    def gitVar = checkout([
        $class: 'GitSCM',
        branches: [[name: gitBranch]],
        doGenerateSubmoduleConfigurations: false,
        extensions: extensions,
        submoduleCfg: [],
        userRemoteConfigs: [[
            credentialsId: 'git',
            url: gitUrl
        ]]
    ])
    return gitVar
}

def copyCredentials(srcStaticParentFolder, repositoryUrlName){
    def srcStaticFolderName = 'staticD'
    def targetFolder = 'src/test/resources/'

    println "evaluating folder structure based on repositoryUrlName=${repositoryUrlName}"
    println "searching static folder in ${srcStaticParentFolder}"

    if(repositoryUrlName == 'purpdm') {
        // copy to folder structure of purpdm main project
        echo '==> considering credentials file and folder structure for PURPDM'
        targetFolder = 'conf/test/resources'
    } else if(repositoryUrlName == 'purpdm-wraptor') {
        // for wraptor another credentials file is used
        echo '==> considering credentials file and folder structure for PURPDM Wraptor'
        srcStaticFolderName = 'staticD_Wraptor'
    } else {
        // folder structure for purpdm modules
        echo '==> considering credentials file and folder structure for any PURPDM module'
    }

    copyFile("${srcStaticParentFolder}/${srcStaticFolderName}/credentials.properties", "${targetFolder}")
}

def copyCredentialsForMainProject(staticFolderName){
    copyFile("../../static/${staticFolderName}/credentials.properties", 'conf/main/resources/com/bosch/configuration/env/env-default/')
}

def copyCredentialsForModule(staticFolderName){
    copyFile("../../static/${staticFolderName}/credentials.properties", 'src/main/resources/com/bosch/configuration/env/env-default/')
}

/**
 * Copies a file from the source path to the destination directory. If destination directory does not exist, it will be created.
 *
 * @param sourceFile The path to the source file to be copied.
 * @param destDir The destination directory where the file will be copied.
 */
def copyFile ( sourceFile, destDir ) {
    if (isUnix()) {
        // create destDir if not exists and copy file, mkdir will also succeed if destDir already exists
        println "Copying source '${sourceFile}' to '${destDir}'"
        sh "mkdir -p ${destDir} && cp -v ${sourceFile} ${destDir}"
    } else {
        def windowsSourceFile = sourceFile.replace('/', '\\')
        def windowsDestDir = destDir.replace('/', '\\')
        println "Copying source '${windowsSourceFile}' to '${windowsDestDir}'"
        bat "xcopy /Y /F /I ${windowsSourceFile} ${windowsDestDir}"
    }
}

def printEnvironmentConfiguration(env) {
    def configString = ''
    env.getEnvironment().each { name, value -> configString += "\n\t$name='$value'" }
    println "Configured environment variables: ${configString}"
}

def printParamsConfiguration(params) {
    def configString = ''
    params.each { name, value -> configString += "\n\t$name='$value'" }
    println "Provided parameters: ${configString}"
}

def evaluateXPath(String xml, String xpathQuery) {
    def xpath = XPathFactory.newInstance().newXPath()
    def documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    def inputStream = new ByteArrayInputStream(xml.bytes)
    def documentElement = documentBuilder.parse(inputStream).documentElement
    xpath.evaluate(xpathQuery, documentElement)
}

def extractReleasedVersionFromReleaseProperties() {
    println "Extracting released version from release.properties in workspace"
    def releaseProps = readProperties file: 'release.properties'
    def version = null
    releaseProps.each { key, value ->
        if (key.startsWith("project.rel.")) {
            version = value
        }
    }

    if (version) {
        println "Release Version: ${version}"
    } else {
        println "Release Version not found"
    }

    return version
}


def createBuildDescription(gitUrl, gitBranch, gitCommit='') {
    println "creating build description out of gitUrl='${gitUrl}', gitBranch='${gitBranch}', gitCommit='${gitCommit}'"

    def repoUrl = normalizeRepoUrl(gitUrl)
    def commitSha = gitCommit
    def commitShort = (commitSha.length() > 11) ? commitSha[0..10] : commitSha
    def branchName = cleanBranchName(gitBranch)

    def matcher = (repoUrl =~ /(https?:\/\/[^\/]+)\/scm\/([^\/]+)\/([^\/]+)(?:\/(?:commit|commits)\/([^\/]+))?/)
    def commitUrl
    def branchUrl
    def encodedAt = encodeRefsHeads(branchName)

    if (matcher.find()) {
        def host = matcher.group(1)
        def project = matcher.group(2)
        def repo = matcher.group(3)
        commitUrl = "${host}/projects/${project}/repos/${repo}/commits/${commitSha}"
        branchUrl = "${host}/projects/${project}/repos/${repo}/browse?at=${encodedAt}"
    } else {
        commitUrl = "${repoUrl}/commits/${commitSha}"
        branchUrl = "${repoUrl}/branches/${encodedAt}"
    }

    println "Commit URL: ${commitUrl}"
    println "Branch URL: ${branchUrl}"

    return "<b>Branch:</b> <a href=\"${branchUrl}\" target=\"_blank\">${branchName}</a><br/><b>Commit:</b> <a href=\"${commitUrl}\" target=\"_blank\">${commitShort}</a>"
}

def normalizeRepoUrl(url) {
    return url.replaceAll(/\.git$/, '').replaceAll(/\/+$/, '')
}

def cleanBranchName(raw) {
    return raw.replaceAll(/^(?:origin\/|refs\/heads\/|remotes\/origin\/)/, '')
}

def encodeRefsHeads(branch) {
    // URL-encode "refs/heads/<branch>" and ensure slashes become %2F
    return java.net.URLEncoder.encode("refs/heads/${branch}", 'UTF-8')
            .replaceAll('\\+', '%20')
            .replaceAll('/', '%2F')
}

def executeShellCommand(command, boolean returnStdout = false) {
    executeShellCommand(command, command, returnStdout)
}

/**
 * Executes a shell command based on the operating system.
 *
 * @param commandUnix The command to execute on Unix systems.
 * @param commandWindows The command to execute on Windows systems.
 * @param returnStdout A boolean flag indicating whether to return the command's output as a string.
 *                     Defaults to false, meaning the command's output is not returned but printed to the console.
 * @return The trimmed output of the command if `returnStdout` is true; otherwise, null.
 */
def executeShellCommand(commandUnix, commandWindows, boolean returnStdout = false) {
    if (isUnix()) {
        println "Executing Unix command: ${commandUnix}"
        def result = sh(script: commandUnix, returnStdout: returnStdout)
        if(result!=null){
            return result.trim()
        }else{
            return null
        }
    } else {
        println "Executing Windows command: ${commandWindows}"
        def result = bat(script: commandWindows, returnStdout: returnStdout)
        if(result!=null){
            return result.trim().readLines().drop(1).join(" ")
        }else{
            return null
        }
    }
}

def callMetronService(jobname, buildId, buildUrl, buildResult, buildStartTime, buildEndTime, nodeName, gitBranch, gitUrl, stageInfo, buildFailureReason, buildTimeInQueue, gitCommitId){
    println "Sending data to metron service for \n\tbuildUrl=${buildUrl} \n\trepo=${gitUrl} \n\tgitBranch=${gitBranch} \n\tcommitId=${gitCommitId}"
    try {
        kpiFramework(
                jobName: jobname,
                buildId: buildId,
                buildUrl: buildUrl,
                status: buildResult,
                startTime: buildStartTime,
                endTime: buildEndTime,
                projectName: 'Purchasing_Galaxy',
                nodeName: nodeName,
                branchName: gitBranch,
                repoName: gitUrl,
                stagesInfo: stageInfo,
                kpiFrameworkEndpointUrl: 'https://metron-prod.de.bosch.com/Purchasing_Galaxy/jenkins',
                onPremise: true,
                failureReason: buildFailureReason,
                timeInQueue: buildTimeInQueue,
                ignoreException: true,
                commitId: gitCommitId
        )
        println 'Data successfully send to Metron service'
    } catch (err) {
        println 'Error during reporting KPIs to Metron. ' + err.toString()
    }
}

/**
 * Fetches JSON response from the given URL using HTTP Basic authentication and parses it into a
 * standard Groovy Map. If any error occurs during the process, null is returned.
 *
 * @param url The endpoint URL returning JSON.
 * @param username The basic auth username.
 * @param password The basic auth password.
 * @return A serializable Map representation of the JSON response, or null if an error occurs.
 */
def getMapFromFromJsonUrl (String url, username, password) {
    def connection = new URL(url).openConnection()
    try{
        print("Connecting to URL: ${url}")
        def auth = getUsernamePasswordEncoded(username, password)
        connection.setRequestProperty('Authorization', "Basic ${auth}")
        connection.setRequestProperty('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)')
        connection.setConnectTimeout(10000) // 10 seconds
        connection.setReadTimeout(10000) // 10 seconds
        connection.connect()

        def inputStream = connection.getInputStream()
        try {
            def json = new JsonSlurper().parse(inputStream)
            //avoid java.io.NotSerializableException: groovy.json.internal.LazyMap
            return toSerializableMap(json)
        }catch (Exception e) {
            print("Error reading inputstream from ${url}, message: ${e}")
        } finally {
            if (inputStream != null){
                inputStream.close()
            }
        }
    } catch (Exception e) {
        print("Error connecting to ${url}, message: ${e}")
    } finally {
        if (connection != null) {
            connection.disconnect()
        }
    }
}

def toSerializableMap (map) {
    def result = [:]
    map.each { k, v ->
        if (v instanceof Map) {
            result[k] = toSerializableMap(v)
        } else if (v instanceof List) {
            result[k] = v.collect { item ->
                if (item instanceof Map) {
                    return toSerializableMap(item)
                } else {
                    return item
                }
            }
        } else {
            result[k] = v
        }
    }
    return result
}

/**
 * Ensures the service instance is up and running and has the expected paramter values.
 * This method blocks until service instance is reachable.
 * *
 * On a missing key or mismatched value this method will fail
 * the pipeline.
 *
 * @param configUrl URL pointing to the configuration endpoint returning JSON reponse
 * @param username basic auth username for the request
 * @param password basic auth password for the request
 * @param expectedParameterValues Map\<String, Object\> of expected key -> value pairs
 * @param timeoutMinutes maximum minutes to wait for a non-empty configuration (defaults to 30)
 */
def ensureAplicationConfigParameters(String configUrl, String username, String password, Map expectedParameterValues, int timeoutMinutes = 30) {
    println "Ensuring application configuration parameters from URL: ${configUrl}"
    def applicationConfigParameters
    try {
        timeout(time: timeoutMinutes, unit: 'MINUTES') {
            while (applicationConfigParameters == null) {
                applicationConfigParameters = getMapFromFromJsonUrl(configUrl, username, password)
                if (applicationConfigParameters == null || applicationConfigParameters.isEmpty()) {
                    sleep time: 60, unit: 'SECONDS'
                }
            }
        }
    }
    catch (error) {
        print("Error while trying to get configuration from ${configUrl}")
        throw error
    }

    for (entry in expectedParameterValues) {
        def configKey = entry.key
        def expected = entry.value

        if(!applicationConfigParameters.containsKey(configKey)){
            error("Configuration key '${configKey}' wasn't present. Expected value was '${expected}'")
        }

        def actual = applicationConfigParameters[configKey]
        if (actual == expected) {
            print("Configuration key '${configKey}' has expected value '${expected}'")
        } else {
            error("Configuration key '${configKey}' hasn't expected value '${expected}' but instead '${actual}'")
        }
    }
}

def getBasicAuthHeader(def token) {
    return getBasicAuthHeader(token, '')
}

def getBasicAuthHeader(def username, def password){
    def auth =  getUsernamePasswordEncoded(username, password)
    return [name:'Authorization', value:"Basic ${auth}"]
}

def getUsernamePasswordEncoded(def username, def password){
    return "${username}:${password}".getBytes('UTF-8').encodeBase64().toString()
}

def simpleEcho(){
    success {
        script {
            echo "SUCCESS"
        }
	}        
    failure {
        script {
            echo "TFAILURE"
        }
    }
    always{
        script {
            simpleEcho()
        }
    }
}