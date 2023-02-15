# Running a release

Prepare the release using the following command:

```sh
mvn clean release:prepare -Prelease
```

The release plugin is configured to now automatically push the commits.
After you ran this, manually push the tag and main to the repository:

```sh
git push upstream main
git push upstream <TAG-NAME>
```

# Uploading a release

Run the following command, after the release is prepared:


```sh
mvn release:perform -Prelease
```


and go to the [repository](https://s01.oss.sonatype.org/) to deploy your component.


Notice: you need authorization from sonatype to have your user authorized.
