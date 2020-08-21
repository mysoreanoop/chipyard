package redefine

import chisel3._

import freechips.rocketchip.config.{Parameters}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.{DontTouch}

class REDEFINESystem(implicit p: Parameters) extends REDEFINESubsystem
  with HasAsyncExtInterrupts
  with CanHaveMasterAXI4MemPort
  with CanHaveMasterAXI4MMIOPort
  with CanHaveSlaveAXI4Port
  with HasPeripheryBootROM
	//traits/mixins
{
  override lazy val module = new REDEFINESystemModuleImp(this)
}

class REDEFINESystemModuleImp[+L <: REDEFINESystem](_outer: L) extends REDEFINESubsystemModuleImp(_outer)
  with HasRTCModuleImp
  with HasExtInterruptsModuleImp
  with HasPeripheryBootROMModuleImp
  with DontTouch

