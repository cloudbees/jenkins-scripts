/*
We had to write this script several times. Time to have it stored, it is a very simple approach but will serve as starting point for more refined approaches. 
*/
Jenkins.instance.getAllItems(Job).each(){ job -> job.isBuildable()

  if (job.isBuilding()){

    def myBuild= job.getLastBuild()

    def runningSince= groovy.time.TimeCategory.minus( new Date(), myBuild.getTime() )

    if (runningSince.hours >= 24){

       println job.name +"---- ${runningSince.hours} hours:${runningSince.minutes} minutes"
    }
  }
}
return null
