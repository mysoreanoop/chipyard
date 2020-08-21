package redefine

import firrtl.options.{StageMain}
import redefine.stage.REDEFINEStage

object Generator extends StageMain(new REDEFINEStage)
