# Preface #

This document describes the functionality provided by the XLD Deployed Artifact steps plugin

See the **XL Deploy Reference Manual** for background information on XL Deploy and deployment concepts.

# Overview #

In 80% of the use case, an upload deployment rule copies the associated artifact to a remote dir without to give anymore extra parameters. If we are using an Un*x shell syntax, it will be : ```scp -r myfolder ssh@machine:/opt/mymiddleware```.
Moreover, the folder can be very large and it'll be smart to update only the created and modified file and removed the extra files.

So this plugin offers 2 new steps:
 
* `<upload-artifact>` to upload and delete an artifact to a remote directory with a rule using a 'deployed' scope mode
* `<delete-artifact>` to delete an artifact from a remote directory with a rule using a 'deployed' scope mode
 
# Step parameters

### upload-artifact

| Parameter        | Type           | Description  | Required |
| ------------- |:-------------:| :-----| ---:|
| description | String | Step description | Yes |
| order | integer | Step order | Yes |
| targetHost | Host | A target host where the step is applied | Yes |
| targetPath | String | Path of the file or folder where the artifact has been uploaded. | Yes |
| artifact | Artifact | current artifact | Yes |
| previousArtifact | Artifact | previous current artifact | No |
| shared-target | boolean | Tell the target directory is shared | No |
| upload-only | boolean | Copy only the new and updated files, leave the missing files as is, default false | No |
| optimized-diff | boolean | Optimize the diff directory process | No |

### delete-artifact

| Parameter        | Type           | Description  | Required |
| ------------- |:-------------:| :-----| ---:|
| description | String | Step description | Yes |
| order | integer | Step order | Yes |
| targetHost | Host | A target host where the step is applied | Yes |
| targetPath | String | Path of the file or folder where the artifact has been uploaded. | Yes |
| previousArtifact | Artifact | previous current artifact | No |
| shared-target | boolean | Tell the target directory is shared | No |

# Requirements #

* **Requirements**
	* **XL Deploy** 6.0.0+
	
# Special Thanks #

The code to manage the diff upload is based on the https://github.com/xebialabs-community/overthere-pylib project
Thank you @ravan.

# Installation #

* Place the plugin JAR file into your `SERVER_HOME/plugins` directory.
* To improve performances,  Add this line to the log configuration conf/logback.xml

```
<logger name="com.xebialabs.overthere.ssh.SshSftpFile" level="error" />
```

# Out-of-box Usage #

If you want to quickly test the plugin, there is a new deployable type `file.LargeFolder' that targets a remote host. It is using the new steps.

```
<type type="file.DeployedLargeFolder" extends="udm.BaseDeployedArtifact" deployable-type="file.LargeFolder" container-type="overthere.Host">
    <generate-deployable type="file.LargeFolder" extends="udm.BaseDeployableFolderArtifact"/>
    <property name="targetPath" description="Path to which artifact must be copied to on the host."/>
    <property category="Advanced" name="targetPathShared" kind="boolean" default="false" required="false" description="Is the targetPath shared by others on the host. When true, the targetPath is not deleted during undeployment; only the artifacts copied to it."/>
    <property category="Advanced" name="uploadOnly" kind="boolean" default="false" required="false" description="Is the targetPath shared by others on the host. When true, the targetPath is not deleted during undeployment; only the artifacts copied to it."/>
</type>
```

# Custom Usage #


synthetic.xml file.

```
<type type="a.Container" extends="generic.Container">
<property name="home" default="/tmp/a/container"/>
</type>

<type type="a.DeployedFileArtifact" extends="udm.BaseDeployedArtifact" deployable-type="a.File" container-type="a.Container">
<generate-deployable type="a.File" extends="udm.BaseDeployableFileArtifact" />
</type>

<type type="a.DeployedFolderArtifact" extends="udm.BaseDeployedArtifact" deployable-type="a.Folder" container-type="a.Container">
<generate-deployable type="a.Folder" extends="udm.BaseDeployableFolderArtifact" />
</type>
```

xl-rules.xml file.

```
<rule name="a.deploy.file" scope="deployed">
<conditions>
  <type>a.DeployedFileArtifact</type>
  <type>a.DeployedFolderArtifact</type>
  <operation>CREATE</operation>
  <operation>MODIFY</operation>
</conditions>
<steps>
  <upload-artifact>    
    <target-path expression="true">deployed.container.home</target-path>
  </upload-artifact>
</steps>
</rule>
<rule name="a.undeploy.file" scope="deployed">
<conditions>
  <type>a.DeployedFileArtifact</type>
  <type>a.DeployedFolderArtifact</type>
  <operation>DESTROY</operation>
</conditions>
<steps>
  <delete-artifact>   
    <target-path expression="true">previousDeployed.container.home</target-path>
  </delete-artifact>
</steps>
</rule>
```


