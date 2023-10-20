## 2023. 10. FHIR Terminology Server with HAPI FHIR 
## 1. Function  
##   1) Terminology Server can be input ImplementGuides what your project fixed very easly and quickly
##       - this Function is provide using your server Disk.
##       - maybe you want to input ImplementGuide in InterNet Or Somthing, using kind of clinet Tool
##         For example) IF you want to use US core then first you need to download that IG(tar) and input the tar in your server.
##                      and then setting the application.yaml in this project
##                      service.terminology.ig.location, exmaplocation.

##   2) Terminology Server can be used normal patterns kind like search CodeSystem, ValueSet and other things
##      of coursly, Terminology server can not provided full search service. 
##      if you want to really need to that, can be modify configuration(TerminologyInterceptor)
##      

##   3) Terminology is not using multi-tenncy and authrization , just public service but if you need, config that kind of those below pattern.


##   4) 