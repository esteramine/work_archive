//Get all issue
def issues = []
def storypointvalue = []
def percentage = []
def startIndex = 0
def size = 10000
while (true) {
    def partialIssues = get ("rest/api/3/search?jql=project%20%3D%20"+project+"%20&startAt=${startIndex}&maxResults=${size}").asString().body
    def issueRes = slurper.parseText(partialIssues)
    if (issueRes.issues.size() == 0) break
    issues += issueRes.issues
    storypointvalue += issueRes.issues.fields[StorypointsField]
    percentage += issueRes.issues.fields[percentageField]
    size = issueRes.issues.size()
    startIndex += size
    
}

// def allissue = get ("rest/api/3/search?jql=project%20%3D%20"+project+"%20").asString().body
// def issue = slurper.parseText(allissue);

// def issuetype=issue.issues.fields.issuetype.name
// def storypointvalue=issue.issues.fields[StorypointsField]
// def percentage = issue.issues.fields[percentageField]

println storypointvalue
println storypointvalue.size()
println percentage
println percentage.size()

// def issues = issue.issues

int level(String name) { // TODO: soft code (get level from json result)
    if (name == 'Phase') return 4
    if (name == 'SDLC') return 3
    if (name == 'Epic') return 2
    if (name == 'Story') return 1
    if (name == 'Sub-task') return 0
}

// save to different maps
def phases = [:]
def sdlcs = [:]
def epics = [:]
def stories = [:]
def subtasks = [:]

// save only list position to map
for (int i = 0; i < issues.size(); i++) {
    if (level(issues[i].fields.issuetype.name) == 4) {
        phases[issues[i].id] = i
    }
    else if (level(issues[i].fields.issuetype.name) == 3) {
        sdlcs[issues[i].id] = i
    }
    else if (level(issues[i].fields.issuetype.name) == 2) {
        epics[issues[i].id] = i
    }
    else if (level(issues[i].fields.issuetype.name) == 1) {
        stories[issues[i].id] = i
    }
    else if (level(issues[i].fields.issuetype.name) == 0) {
        subtasks[issues[i].id] = i
    }
}

stories.each { key, val ->
    println "Story: $key, Issue position: $val"
    def completion = 0
    if (issues[val].fields.subtasks.size() > 0) { // has children, completion depends on children
        def total = 0
        def done = 0
        for (id in issues[val].fields.subtasks.id) {
            println "Subtask id: $id, Story points: ${storypointvalue[subtasks[id]]}, Status: ${issues[subtasks[id]].fields.status.name}"
            total += storypointvalue[subtasks[id]]?:1
            if (issues[subtasks[id]].fields.status.name == "Done") done += storypointvalue[subtasks[id]]?:1
        }
        for (id in issues[val].fields.subtasks.id) {
            // TODO: assign the percentage back to subtasks
            // save a new field for story to record their completion record by 'val['completion']=...'
            
        }
        completion = (done ==0 || total==0)? 0:done*100/total
    }
    else { // no children, completion depends on itself
        completion = (issues[val].fields.status.name == "Done")? 100:0
    }
    // completion = (done ==0 || total==0)? 0:done*100/total
    println "Completion: ${completion}%"
    
    // update parent percentage
    if (issues[val].fields.parent) {
        def epicIndex = epics[issues[val].fields.parent.id]
        if (!issues[epicIndex]['total']) issues[epicIndex]['total'] = 0
        issues[epicIndex]['total'] += completion * percentage[val]/100  // multiplied by story's percentage
    }
    
    println ""
}

epics.each { key, val -> 
    println "Epic: $key, Issue position: $val, Completion: ${issues[val].total}%"
    // TODO: 1. update to jira (call api), use the 'total' field (remember to check 'null' because there may be epics having no stories)
    
    // 2. update SDLC
    def index = sdlcs[issues[val].fields.issuelinks[0].outwardIssue.id] // *have to check whether all issuelinks only have one children (outwardIssue = parent)
    if (!issues[index]['total']) issues[index]['total'] = 0 // sdlc hasn't has a 'total' property yet, add one
    def epicCompletion = issues[val].total?:(issues[val].fields.status.name == "Done"? 100:0) // if epic no 'total' property means the epic has no stories (children)
    issues[index]['total'] += epicCompletion * percentage[val]/100  // multiplied by epic's percentage (* epics total percentage is not 100%)
    println "Total Contribution to SDLC: ${issues[index].total}%"
    println ""
}

sdlcs.each { key, val -> 
    println "SDLC: $key, Issue position: $val, Completion: ${issues[val].total}%"
    // TODO: 1. update to jira (call api), use the 'total' field (remember to check 'null' because there may be sdlcs having no epics)
    
    // 2. update Phase
    def parent = issues[val].fields.issuelinks.findAll{it.outwardIssue != null}[0].outwardIssue
    println "Phase id: ${parent.id}, Phase position: ${phases[parent.id]}"
    def index = phases[parent.id] // *have to check whether all issuelinks only have one children (outwardIssue = parent)
    if (!issues[index]['total']) issues[index]['total'] = 0
    def sdlcCompletion = issues[val].total?:(issues[val].fields.status.name == "Done"? 100:0) // if epic no 'total' property means the epic has no stories (children)
    issues[index]['total'] += sdlcCompletion * percentage[val]/100  // multiplied by sdlc's percentage (* sdlccs total percentage is not 100%)
    println "Total Contribution to Phase: ${issues[index].total}%"
    println ""
}

def thread = Thread.start {
  sleep(2)
  println "new thread"
}

thread.join()
println "hi"




