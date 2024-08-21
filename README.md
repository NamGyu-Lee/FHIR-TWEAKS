### FHIR TWEAKS(Transform data With Easy And Kind Services)

Based on HAPI FHIR 6.8.3 (https://github.com/hapifhir/hapi-fhir-jpaserver-starter).
This tool is released as a study to help various institutions, companies, and individuals, including hospitals that find FHIR challenging, to easily and quickly utilize and transform their data.
By leveraging this library, EMR systems and various medical data transformation tasks can be easily established with the following functionalities implemented as libraries.

#### 1. Data Transform
#### 2. Terminology Server
#### 3. FHIR Repository with APM(with PinPoint)
#### 4. Easy-to-Use Validation Service

---
1. Data Transform
#### This functionality started from a study on how to transform EMR data into FHIR quickly and easily while maintaining its structure.

The requirements to utilize this tool are as follows:

* 1. StructureDefinition of the FHIR IG (Implementation Guide) to be transformed
* 2. A basic understanding of FHIR
* 3. A detailed understanding of the data used in the EMR system you wish to transform
* 4. Hardware with specifications capable of running HAPI FHIR properly
After forking this project, use Docker-compose to start the server and utilize it as follows:

```bash
docker-compose up -d
```

If it is active, then read this page for use it.

Tutorial page this here : [tutorial pages](tutorial/transform/1.introduction.md)

※ For detailed functionality and operation methods, please refer to the research paper below.

Research paper link: ~~[Research Page](not)~~

---

2. Terminology Server
#### If you need to configure the FHIR Terminology server, refer to the following source in the application.yaml

```
service:
  terminology:
    ig:
      location: /app/ig/structure
      examplelocation: /app/ig/example
    common:
      ig: # Due to the heavy load during the process of adding the Implementation Guide (IG), a timeout has been added
        timeout: 3000000
      paging:
        defaultsize: 100
        maxsize: 1000
        fifosize: 320
      search:
        summary:
          codesystem: true
          valueset: true
          searchparameter: true
```


#### You can configure the Implementation Guide by uploading it from a local or external server using the following settings.

---

3. FHIR Repository with APM (PinPoint)
#### if you want to use the APM check the dockerfile in this project

```
...
COPY pinpoint-agent-2.5.3 /pinpoint-agent
ENTRYPOINT ["java", "-javaagent:/pinpoint-agent/pinpoint-bootstrap-2.5.3.jar", "-Dpinpoint.agentId=fhir-web", "-Dpinpoint.applicationName=PHIS-FHIR-Server", "--class-path", "/app/main.war", "-Dloader.path=main.war!/WEB-INF/classes/,main.war!/WEB-INF/,/app/extra-classes", "org.springframework.boot.loader.PropertiesLauncher"]
```

#### All settings have already been configured, and you can proceed as described above. For low-performance Server or Client, you can replace the line with the one commented out above

---

4. Easy-to-Use Validation Service

I have already configured the Validation Chain based on the HAPI FHIR Server in this project, and you can easily enable or disable it by adjusting the settings below.

check the application.yaml

```
  validation:
    enabled: false
    local:
      enabled: false
    remote:
      logginglevel : warn
      server:
        enabled: true
        url: http://fhrdev.cmcnu.or.kr/fhir
```
