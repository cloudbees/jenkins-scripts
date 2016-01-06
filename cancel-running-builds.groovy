public int cancelRunning() {
        // Cancel running builds.
        def numCancels = 0;
        for (job in this.hudson.instance.items) {
            for (build in job.builds) {
                if (build == this.build) { continue; } // don't cancel ourself!
                if (!build.hasProperty('causes')) { continue; }
                if (!build.isBuilding()) { continue; }
                for (cause in build.causes) {
                    if (!cause.hasProperty('upstreamProject')) { continue; }
                    if (cause.upstreamProject == this.upstreamProject &&
                            cause.upstreamBuild == this.upstreamBuild) {
                        this.printer.println('Stopping ' + build.toString());
                        build.doStop();
                        this.printer.println(build.toString() + ' stopped.');
                        numCancels++;
                        break;
                    }
                }
            }
        }
        return numCancels;
    }