Jenkins.instance.getAllItems(Job.class).each { it ->
    // This does not work with matrix projects
    if (!(it instanceof hudson.matrix.MatrixConfiguration)) {
        def nextNumber;

        // If the next build number file does not exist then set it to one.
        try {
            nextNumber = it.getNextBuildNumberFile().read().trim();
        } catch (java.io.FileNotFoundException e) {
            nextNumber = 1;
        }

        println("Job: " + it.getFullName() + ". Next Build Number: " + nextNumber + ". ");
        println "Content of file: " + nextNumber;

        def largest = 1;
        def current;
        // For each build directory in the job builds dir
        it.getBuildDir().list().each { builds ->
            try {
                // Get the file name and try to convert to an integer.
                current = Integer.valueOf(new File(builds).getName());
                // if the current directory largest is > largest from memory
                if (current > largest) {
                    // replace largest from memory
                    largest = current;
                }
            } catch (e) {
                // If it fails to convert, move on.
            }
        }

        println "Largest: " + (largest + 1) + " Next Number: " + nextNumber;
        // If the next build number is > largest + 1
        if (!nextNumber.equals(largest + 1)) {
            // Update and save the next build number as the new largest
            it.updateNextBuildNumber(largest + 1);
            println "Wrote content " + it.nextBuildNumber + " to the job: " + it.getFullName();
        }
    } else {
        println "Ignore matrix projects";
    }
};