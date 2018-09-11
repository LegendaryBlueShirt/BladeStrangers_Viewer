package com.justnopoint.`interface`

interface FrameDataProvider {
    fun getCharacters(): List<Character>
    fun loadCharacter(character: Character)
    fun getSequences(): List<Sequence>
    fun getFrameRenderer(): FrameRenderer
}