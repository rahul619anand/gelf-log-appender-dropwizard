# How to deploy this library 

## Before you start

You'll also need to make sure that you have configured your sonatype
login information in your `~/.m2/settings.xml`. Something like:

```xml
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>sonatype-username</username>
      <password>sonatype-password</password>
    </server>
  </servers>
```

## Step 1 - Update README and pom.xml

- Ensure library version is set to the version you want to release, appended with "-SNAPSHOT"
- In Changelog at bottom of README:
  - Add what was changed since the last release

## Step 2 - Publish snapshot 

`mvn clean deploy` 


