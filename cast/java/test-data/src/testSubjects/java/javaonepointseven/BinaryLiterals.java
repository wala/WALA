package javaonepointseven;

/**
 * @author Linghui Luo
 * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/binary-literals.html">Java 7 docs</a>
 */
public class BinaryLiterals {

  public static final int[] phases = {
    0b00110001, 0b01100010, 0b11000100, 0b10001001, 0b00010011, 0b00100110, 0b01001100, 0b10011000
  };

  // An 8-bit 'byte' value:
  private byte aByte = (byte) 0b00100001;

  // A 16-bit 'short' value:
  private short aShort = (short) 0b1010000101000101;

  // Some 32-bit 'int' values:
  private int anInt1 = 0b10100001010001011010000101000101;
  private int anInt2 = 0b101;
  private int anInt3 = 0B101; // The B can be upper or lower case.

  // A 64-bit 'long' value. Note the "L" suffix:
  private long aLong = 0b1010000101000101101000010100010110100001010001011010000101000101L;

  public static void main(String[] args) {
	  BinaryLiterals x = new BinaryLiterals();
	  x.decodeInstruction(463527, x.new State(11));
  }
  
  public State decodeInstruction(int instruction, State state) {

    if ((instruction & 0b11100000) == 0b00000000) {
      final int register = instruction & 0b00001111;
      switch (instruction & 0b11110000) {
        case 0b00000000:
          return state.nop();
        case 0b00010000:
          return state.copyAccumTo(register);
        case 0b00100000:
          return state.addToAccum(register);
        case 0b00110000:
          return state.subFromAccum(register);
        case 0b01000000:
          return state.multiplyAccumBy(register);
        case 0b01010000:
          return state.divideAccumBy(register);
        case 0b01100000:
          return state.setAccumFrom(register);
        case 0b01110000:
          return state.returnFromCall();
        default:
          throw new IllegalArgumentException();
      }
    } else {
      final int address = instruction & 0b00011111;
      switch (instruction & 0b11100000) {
        case 0b00100000:
          return state.jumpTo(address);
        case 0b01000100:
          return state.jumpIfAccumZeroTo(address);
        case 0b01001000:
          return state.jumpIfAccumNonzeroTo(address);
        case 0b01100000:
          return state.setAccumFromMemory(address);
        case 0b10100000:
          return state.writeAccumToMemory(address);
        case 0b11000000:
          return state.callTo(address);
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  class State {
    int state = 0;

    public State(int s) {
      this.state = s;
    }

    public State nop() {
      return null;
    }

    public State copyAccumTo(int register) {
      return new State(1);
    }

    public State addToAccum(int register) {
      return new State(2);
    }

    public State subFromAccum(int register) {
      return new State(3);
    }

    public State multiplyAccumBy(int register) {
      return new State(4);
    }

    public State divideAccumBy(int register) {
      return new State(5);
    }

    public State setAccumFrom(int register) {
      return new State(6);
    }

    public State returnFromCall() {
      return new State(7);
    }

    public State jumpTo(int address) {
      return new State(8);
    }

    public State jumpIfAccumZeroTo(int address) {
      return new State(9);
    }

    public State jumpIfAccumNonzeroTo(int address) {
      return new State(10);
    }

    public State setAccumFromMemory(int address) {
      return new State(11);
    }

    public State writeAccumToMemory(int address) {
      return new State(12);
    }

    public State callTo(int address) {
      return new State(13);
    }
  }
}
