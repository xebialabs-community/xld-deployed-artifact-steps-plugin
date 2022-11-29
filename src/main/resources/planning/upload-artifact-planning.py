context.addStep(steps.upload_artifact(
    order=60,
    target_path=deployed.targetPath,
    shared_target=deployed.targetPathShared,
    upload_only=deployed.uploadOnly,
    optimized_diff=deployed.optimizedDiff,
    description="Write Synchronization {0} on {1}".format(deployed.name, deployed.container.name),
))
