package redefine

import chisel3._
import chisel3.internal.sourceinfo.{SourceInfo}

import freechips.rocketchip.config.{Field, Parameters}
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.devices.debug.{HasPeripheryDebug, HasPeripheryDebugModuleImp}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.model.{OMInterrupt}
import freechips.rocketchip.diplomaticobjectmodel.logicaltree.{RocketTileLogicalTreeNode, LogicalModuleTree}
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._



class REDEFINESubsystem(implicit p: Parameters) extends BaseSubsystem
	with HasRocketTiles
{
  val tiles = rocketTiles

  // add Mask ROM devices
  val maskROMs = p(PeripheryMaskROMKey).map { MaskROM.attach(_, cbus) }

  val hartPrefixNode = if (p(HartPrefixKey)) {
    Some(BundleBroadcast[UInt](registered = true))
  } else {
    None
  }

  val hartPrefixes = hartPrefixNode.map { hpn => Seq.fill(tiles.size) {
   val hps = BundleBridgeSink[UInt]
   hps := hpn
   hps
  } }.getOrElse(Nil)

  override lazy val module = new REDEFINESubsystemModuleImp(this)
}

trait HasRocketTilesModuleImp extends HasTilesModuleImp
    with HasPeripheryDebugModuleImp {
  val outer: HasRocketTiles
}

trait HasRocketTiles extends HasTiles
    with CanHavePeripheryPLIC
    with CanHavePeripheryCLINT
    with HasPeripheryDebug { this: BaseSubsystem =>
  val module: HasRocketTilesModuleImp

  protected val rocketTileParams = p(RocketTilesKey)
  private val crossings = perTileOrGlobalSetting(p(RocketCrossingKey), rocketTileParams.size)

  // Make a tile and wire its nodes into the system,
  // according to the specified type of clock crossing.
  // Note that we also inject new nodes into the tile itself,
  // also based on the crossing type.
  val rocketTiles = rocketTileParams.zip(crossings).map { case (tp, crossing) =>
    val rocket = LazyModule(new RocketTile(tp, crossing, PriorityMuxHartIdFromSeq(rocketTileParams), logicalTreeNode))

    connectMasterPortsToSBus(rocket, crossing)
    connectSlavePortsToCBus(rocket, crossing)
    connectInterrupts(rocket, debugOpt, clintOpt, plicOpt)

    rocket
  }

  rocketTiles.map {
    r =>
      def treeNode: RocketTileLogicalTreeNode = new RocketTileLogicalTreeNode(r.rocketLogicalTree.getOMInterruptTargets)
      LogicalModuleTree.add(logicalTreeNode, r.rocketLogicalTree)
  }

  def coreMonitorBundles = (rocketTiles map { t =>
    t.module.core.rocketImpl.coreMonitorBundle
  }).toList
}



class REDEFINESubsystemModuleImp[+L <: REDEFINESubsystem](_outer: L) extends BaseSubsystemModuleImp(_outer)
  with HasResetVectorWire
	with HasRocketTilesModuleImp
{
  for (i <- 0 until outer.tiles.size) {
    val wire = tile_inputs(i)
    val prefix = outer.hartPrefixes.lift(i).map(_.bundle).getOrElse(0.U)
    wire.hartid := prefix | outer.hartIdList(i).U
    wire.reset_vector := global_reset_vector
  }
}
