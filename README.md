# maven-universal-module-aggregator-plugin
maven plugin who aggregate files from the module to the target of the parents

##How to use:

add in the pom of your parent:

````
<build>
    <plugins>

        <plugin>
            <groupId>com.github.sarahbuisson</groupId>
            <artifactId>maven-universal-module-aggregator-plugin</artifactId>
            <version>LATEST</version>
            <configuration>
                <filesToAggregateModulePath>someDir</filesToAggregateModulePath>
           <!--... --> 
            </configuration>
        </plugin>
    </plugins>
</build>
````

call the goal you want to aggregate, then the universal-module-aggregator:aggregate:
```
mvn universal-module-aggregator:aggregate

```

you will find in the parent target a copy of all the files of all your module:
```
cd target;

>module1
>    target
>        someDir
>            fileModule1.html
>module2
>    target
>        someDir
>            fileModule2.html
>target
>    someDir
>        index.html
>        module1:
>            fileModule1.html
>        module2:
>            fileModule2.html

```
##configuration options

**filesToAggregateModulePath**: Mandatory

directory in the modules's target where the files will be taken. 



**reportsDirectory**: Default **filesToAggregateModulePath** 

directory in the target parent where the files of the modules will be put. By default take the same value as **filesToAggregateModulePath** 
   
   
**aggregateTemplatePath** Optional path in

**copyModules** Default:true
 
copy the modules into the target parent
configuration example: 
```
     <configuration>
                            <reportsDirectory>pit-reports</reportsDirectory>
                            
                        </configuration>
```