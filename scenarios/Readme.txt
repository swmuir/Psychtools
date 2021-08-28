Scenario 1

Conditions Confirma

The following Scenario is meant to describe a point in time associated with process flow of the application.  It is meant to be illustrative of the set of FHIR resources to describe the state 


State
Veterans was serviced at ED where a provisional diagnosis of PTSD was recorded. There was a followup appointment with Psych Provider where the initial PTSD was confirmed and an additional diagnosis of Depression was also diagnosed.  The result of the encounter was to have N additional therapy sessions (??) 

Actors
Veteran
Intake(Initial) Provider  - the provider who provided initial diagnosis which triggered the encounter
Encounter Provider - The provider who executed the encounter 


FHIR Resources

Patient(1)
Practitioner(2)
Encounter(1)
Condition(3) provisional and two confirmed
EpisodeOfCare
ServiceRequest
Note?? TBD
TBD CareTeam


Scenario 2

Mental Status Exam 

The veteran has been seen by clinician and a "Mental Status Exam"  has been completed 

N number of observations have been recorded 

(Need to pick set of observations) 


Second Encounter 

Eventually a Questionaire and response


Scenario 3


Social Skills Training

Social Skills Training Plan and activity definitions
Veteran has been seen and several activities have been completed


