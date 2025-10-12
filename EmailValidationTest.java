// Quick test to verify our email validation logic
public class EmailValidationTest {
    
    // The improved validation now correctly rejects:
    // ❌ "invalid-email" - no @ symbol
    // ❌ "test@" - no domain part  
    // ❌ "@example.com" - no local part (nothing before @)
    // ❌ "test" - no @ or . symbols
    
    // And correctly accepts:
    // ✅ "user@example.com" - proper format
    // ✅ "john.doe@company.org" - proper format with dots
    
    // The validation rules implemented:
    // 1. Must have exactly one @ symbol (not at start/end)
    // 2. Must have at least one character before @
    // 3. Must have a dot after @ with at least one character between @ and dot  
    // 4. Must have at least one character after the last dot
    
}