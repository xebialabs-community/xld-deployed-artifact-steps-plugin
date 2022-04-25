context.addStep(steps.upload-artifact(
    order=60,
    target-path=deployed.targetPath,
    shared-target=deployed.targetPathShared,
    upload-only=deployed.uploadOnly,
    optimized-diff=deployed.optimizedDiff,
    description="Write Synchronization {0} on {1}".format(deployed.name, deployed.container.name),
))
