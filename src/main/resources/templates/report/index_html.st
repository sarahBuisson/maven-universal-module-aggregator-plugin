<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" type="text/css" href="../style.css">
</head>
<body>
    <h1>Aggregation of $mojo.aggregateTemplatesPath$ by modules</h1>
    <br/>
    <ul>
    $modules:{ module |
        <li>
            <a href="./$module.artifactId$/index.html">$module.artifactId$</a>
       </li>
    }$
    </ul>
</body>
</html>