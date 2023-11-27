## 2023. 10. FHIR Terminology Server with HAPI FHIR 
#### HAPI FHIR 6.8.3(https://github.com/hapifhir/hapi-fhir-jpaserver-starter)을 기반으로 
#### 국내 FHIR서비스에 적절히 맞도록 
#### 쉽게 구축 할 수 있도록 아래의 기능별로 구현하였다.

---
#### 1. FHIR Terminology Service
##### 
   1) Implement Guide 적용
    - 기본적인 IG 적용 패턴 및 방법 정의
    - 대용량 CodeSystem의 대한 적용
    - 원내 자체적인 CodeSystem의 대한 적용

   3) 국내 의료환경에 부합하는 ConceptMap 구성

   4) MSA 기반의 Terminology 서비스 구성

---
#### 2. FHIR Validation Service
##### 
   1) Validation 적용 및 활용

   2) MSA 기반의 Validation Server 구성

---
#### 3. FHIR Transform Data Service
#####
   1) Transform Map 구성

   2) Transform 수행

---
#### 4. FHIR Multi-tenant & 접근제어
##### 
   1) Multitenant 관리 및 Database size 관리

   2) 사용자 접근제어 관리

---
#### 5. 사용자 활용
#####
   1) FHIR Data의 대한 User 가독성 처리

---
#### 6. 관리
#####
   1) HAPI FHIR Server 의 대한 Actuator 설정 및 예시

   2) HAPI FHIR Server 의 Search Engine 활용(ElasticSearch)

