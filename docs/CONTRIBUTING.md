# Contributing Guide

## How to Contribute

Thank you for your interest in contributing! This guide explains how to get started.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
```bash
git clone https://github.com/YOUR_USERNAME/job-scheduler.git
cd job-scheduler
```

3. **Create a feature branch**:
```bash
git checkout -b feature/your-feature-name
```

4. **Make your changes** and commit with clear messages
5. **Push to your fork**:
```bash
git push origin feature/your-feature-name
```

6. **Create a Pull Request** on GitHub

## Code Standards

### TypeScript
- Write all frontend code in TypeScript
- Use proper type annotations
- Avoid `any` type (use specific types instead)

### Naming Conventions
- **Components**: `PascalCase` (e.g., `JobCreateAdvanced.tsx`)
- **Variables**: `camelCase` (e.g., `formData`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRIES`)
- **Files**: Match component names

### Code Style
- Use Tailwind CSS for styling (not inline styles)
- Prefer functional components with hooks
- Keep functions under 50 lines
- Extract reusable logic to custom hooks
- Comment complex logic, not obvious code

### File Organization
```
src/
 pages/          # Page components
 components/     # Reusable components
 api/           # API integration
 store/         # State management
```

## Git Commit Messages

Write clear, descriptive commit messages:

```
# Format
<type>: <description>

# Types
feat:   New feature
fix:    Bug fix
docs:   Documentation
style:  Code style (no logic change)
refactor: Code restructure
perf:   Performance improvement
test:   Tests

# Examples
feat: add job creation wizard
fix: resolve CORS issues with proxy
docs: update API documentation
```

## Pull Request Process

### Before Submitting

- [ ] Code follows style guide
- [ ] No console errors or warnings
- [ ] Tested locally with backend running
- [ ] Updated documentation if needed
- [ ] No hardcoded values or secrets
- [ ] No unused imports or variables

### PR Description Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Other

## Testing
How to test these changes:
1. Step 1
2. Step 2
3. etc.

## Screenshots (if applicable)
Add screenshots for UI changes.

## Checklist
- [ ] Code follows style guide
- [ ] No breaking changes
- [ ] Documentation updated
- [ ] Tests added/updated
```

## Feature Development

### Adding a New Feature

1. **Create an issue** describing the feature
2. **Discuss approach** before writing code
3. **Implement following guidelines**:
   - Write TypeScript with proper types
   - Use Tailwind CSS for styling
   - Test with backend running
   - Add error handling

4. **Update documentation**:
   - Add to relevant `.md` files
   - Include examples
   - Update API reference if needed

5. **Submit PR** with clear description

### Feature Checklist

- [ ] New feature works as described
- [ ] All required fields are validated
- [ ] Error handling is graceful
- [ ] Loading states are shown
- [ ] Works on mobile view
- [ ] Keyboard navigation works
- [ ] Documentation is updated
- [ ] No console errors

## Bug Fixes

1. **Create an issue** describing the bug
2. **Write a test case** that reproduces it
3. **Fix the bug** (commit message: `fix: description`)
4. **Verify test passes**
5. **Submit PR** with issue reference: `Fixes #123`

## Documentation Contributions

### Updating Docs

1. Files are in `/docs` folder
2. Main README is `/README.md`
3. Write clear, concise documentation
4. Include examples where helpful
5. Use consistent formatting

### Documentation Guidelines

- Use clear, simple language
- Include code examples
- Include screenshots for UI changes
- Link to related docs
- Update table of contents if needed
- Check for typos and grammar

## Code Review Process

### What Reviewers Look For

- **Correctness**: Does the code work?
- **Style**: Does it follow conventions?
- **Performance**: Are there efficiency concerns?
- **Security**: Are there potential issues?
- **Testing**: Is it adequately tested?
- **Documentation**: Is it documented?

### Responding to Review Comments

1. Read comments carefully
2. Ask for clarification if needed
3. Make requested changes
4. Commit with clear message: `Address review feedback`
5. Re-request review

### Don't Take It Personally

- Reviews are about code, not you
- Questions are for understanding
- Suggestions are to improve quality
- Aim for collaborative improvement

## Areas We Need Help With

### High Priority
- [ ] Performance optimization
- [ ] Additional job types
- [ ] Better error handling
- [ ] Unit tests
- [ ] E2E tests

### Medium Priority
- [ ] Dark mode improvements
- [ ] Accessibility enhancements
- [ ] Mobile UI refinements
- [ ] Documentation expansion
- [ ] Example configurations

### Nice to Have
- [ ] Advanced scheduling features
- [ ] Data export/import
- [ ] Webhook support
- [ ] API tokens
- [ ] Metrics export

## Questions or Need Help?

- Check existing issues (might be answered)
- Ask in PR comments
- Create a discussion
- Review relevant documentation

## Code of Conduct

- Be respectful and inclusive
- No harassment or discrimination
- Welcome diverse perspectives
- Assume good intentions
- Resolve conflicts respectfully

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Recognition

Contributors will be:
- Added to CONTRIBUTORS.md
- Mentioned in release notes
- Acknowledged for significant work

Thank you for contributing!‰
